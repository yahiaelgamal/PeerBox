package PeerBox;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import networking.ServerClient;

import org.json.simple.parser.ParseException;

import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class ChordWrapper {

	// use over real network
	// public static String PROTOCOL =
	// URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

	// use for testing on the JVM/thread
	public static String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);
	// Used for networking p2p 
	public static int NETWORKING_PORT = 5678;

	public Chord dht1;
	public Chord dht2;
	public FileManager fileManager;
	public ServerClient networking;
	

	// In case of a creator
	public ChordWrapper(URL myURL1, URL myURL2, String myFolder) {
		try {
			this.dht1 = new ChordImpl();
			this.dht1.create(myURL1);

			this.dht2 = new ChordImpl();
			this.dht2.create(myURL2);

			this.fileManager = new FileManager(myFolder);
			
			this.networking = new ServerClient(NETWORKING_PORT, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// In case of bootstraper
	public ChordWrapper(URL myURL1, URL myURL2, URL bootstrapURL1,
			URL bootstrapURL2, String myFolder) {
		try {
			this.dht1 = new ChordImpl();
			this.dht1.join(myURL1, bootstrapURL1);

			this.dht2 = new ChordImpl();
			this.dht2.join(myURL2, bootstrapURL2);

			this.fileManager = new FileManager(myFolder);
			
			this.networking = new ServerClient(NETWORKING_PORT, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// splits file into pieces, hashes and encrypts each, and inserts them into
	// DHT1. generates torrent JSON string, hashes and encrypts it and inserts
	// it into DHT2
	// returns torrent info in the form
	// {String torrentHash, String torrentKey, String iv}
	public String[] uploadFile(String filename) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidParameterSpecException,
			IllegalBlockSizeException, BadPaddingException, ServiceException {

		// generate key
		byte[] secretKey = Crypto.generateAESSecret();

		// hash and encrypt torrent file and insert encrypted into dht2(hash)
		Object[] torrentInfoAndEncTorrent = insertPiecesAndGenTorrent(filename, secretKey);
		String[] torrentInfo = (String[]) torrentInfoAndEncTorrent[0]; 
		byte[] encryptedTorrent = (byte[]) torrentInfoAndEncTorrent[1];
		
		String hash = torrentInfo[0];

		dht2.insert(new Key(hash),
				Utils.concat(new byte[] { (byte) 255 }, encryptedTorrent));

		// store torrentInfo
		fileManager.addFile(filename, torrentInfo);

		return torrentInfo;
	}

	public String[] update(String filename, String[] torrentInfo)
			throws IOException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidParameterSpecException,
			IllegalBlockSizeException, BadPaddingException, ServiceException,
			InvalidAlgorithmParameterException, ParseException {

		// get old torrentinfo
		Key key1 = new Key(torrentInfo[0]);
		byte[] secretKey1 = Utils.fromHexString(torrentInfo[1]);
		byte[] iv1 = Utils.fromHexString(torrentInfo[2]);

		// retrieve and decrypt old torrent info
		byte[] dhtEntry = (byte[]) (dht2.retrieve(key1).toArray()[0]);
		byte[] torrentBytes = PeerUtils.splitDHTEntry2(dhtEntry)[1];
		byte[] torrentDecrypted = Crypto.decryptAES(torrentBytes, secretKey1, iv1);
		TorrentConfig torrentJSON = new TorrentConfig(torrentDecrypted);
		ArrayList<ArrayList<String>> hash_key_ivs = torrentJSON
				.getAllPiecesInfo();
		
		// delete old pieces and old torrent file
		deletePieces(hash_key_ivs);
		dht2.remove(key1, dhtEntry);

		// f(keytemp, keytorr) = keynew
		byte[] tempKey = Crypto.generateAESSecret();
		byte[] secretKey2 = PeerUtils.function(tempKey, secretKey1);

		// insert pieces and hash + encrypt new torrent file
		Object[] torrentInfoAndEncTorrent = insertPiecesAndGenTorrent(filename, secretKey2);
		String[] newTorrentInfo = (String[]) torrentInfoAndEncTorrent[0]; 
		byte[] encryptedTorrent = (byte[]) torrentInfoAndEncTorrent[1];
		
		String hash2 = newTorrentInfo[0];
		byte[] iv2 = Utils.fromHexString(newTorrentInfo[2]);
		
		byte[] hash2Bytes = Utils.fromHexString(hash2);
		Key key2 = new Key(hash2);

		fileManager.replaceTorrentInfo(filename, newTorrentInfo);

		// insert in dht2 <key1, 0x00 || tempKey || key2 || iv2>
		// & <key2, 0xff || encTorrent>
		this.dht2.insert(key1,PeerUtils.createDHT2Entry1(tempKey, hash2Bytes, iv2)); 
		this.dht2.insert(key2, PeerUtils.createDHT2Entry2(encryptedTorrent));
		
		return newTorrentInfo;
	}

	// naive download. assumes file has not been edited.
	// downloads file given its torrent info (hash, key and iv)
	private void downloadFile(TorrentConfig torrentConfig) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, ServiceException, IOException,
			ParseException {
		String filename = (String) torrentConfig.get("filename");

		ArrayList<ArrayList<String>> hash_key_ivs = torrentConfig
				.getAllPiecesInfo();

		// download and combine pieces
		downloadFile(filename, hash_key_ivs);
	}

	// gets required pieces from DHT1, decrypts each, and combines them into
	// file with name filename
	private void downloadFile(String filename,
			ArrayList<ArrayList<String>> hash_key_ivs) throws ServiceException,
			IOException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException {
		FileOutputStream fos = new FileOutputStream(
				fileManager.buildFullPath(filename), false);

		byte[] pieceBytes, decryptedBytes;
		for (int i = 0; i < hash_key_ivs.size(); i++) {
			System.out.println("Getting piece " + i);

			ArrayList<String> hash_key_iv = hash_key_ivs.get(i);

			// get piece from DHT using key
			Set<Serializable> set = getPiece1(new Key(hash_key_iv.get(0)));
			pieceBytes = (byte[]) (set.toArray()[0]);

			// secret key and IV
			// consider changing! we convert byte[] to string and back to byte[]
			byte[] secretKey = Utils.fromHexString(hash_key_iv.get(1));
			byte[] iv = Utils.fromHexString(hash_key_iv.get(2));

			// decrypt piece
			decryptedBytes = Crypto.decryptAES(pieceBytes, secretKey, iv);

			// write pieces to file
			fos.write(decryptedBytes);
			fos.flush();
		}
		fos.close();
	}

	// assumes fileManager already has torrentInfo for filename stored
	public void sync(String filename) throws ServiceException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException,
			IOException, ParseException {
		// get hash, secretKey, iv
		String[] torrentInfo = fileManager.getTorrentInfo(filename);

		// download file
		sync(filename, torrentInfo);
	}

	private void sync(String filename, String[] torrentInfo)
			throws ServiceException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, IOException, ParseException {
		String hash = torrentInfo[0];
		byte[] key = Utils.fromHexString(torrentInfo[1]);

		byte[] dht2Entry = (byte[]) dht2.retrieve(new Key(hash)).toArray()[0];

		if (Utils.isTorrentFile(dht2Entry)) {
			// get encrypted torrent info from dht2
			byte[] iv = Utils.fromHexString(torrentInfo[2]);
			byte[] dhtEntry = (byte[]) dht2.retrieve(new Key(hash)).toArray()[0];
			byte[] torrentBytes = PeerUtils.splitDHTEntry2(dhtEntry)[1];

			// decrypt torrent
			byte[] torrentDecrypted = Crypto.decryptAES(torrentBytes, key, iv);
			TorrentConfig torrentConfig = new TorrentConfig(torrentDecrypted);
			
			downloadFile(torrentConfig);
		} else {
			byte[][] splitEntry = PeerUtils.splitDHTEntry1(dht2Entry);
			byte[] tempKey = splitEntry[1];
			byte[] key2 = splitEntry[2];
			byte[] iv2 = splitEntry[3];

			String[] newTorrentInfo = new String[3];
			newTorrentInfo[0] = Utils.toHexString(key2);
			newTorrentInfo[1] = Utils.toHexString(PeerUtils.function(tempKey, key));
			newTorrentInfo[2] = Utils.toHexString(iv2);

			fileManager.replaceTorrentInfo(filename, newTorrentInfo);

			sync(filename, newTorrentInfo);
		}
	}

	// {String[] torrentInfo, byte[] encryptedTorrent}
	private Object[] insertPiecesAndGenTorrent(String filename,
			byte[] secretKey) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidParameterSpecException,
			IllegalBlockSizeException, BadPaddingException, ServiceException {
		// divide to pieces, hash, encrypt and insert into DHT1
		byte[][] pieces = fileManager.splitFiles(filename);
		String[][] pieceInfo = insertPieces(pieces);

		// generate torrent info
		TorrentConfig torrent = new TorrentConfig(filename, pieceInfo);
		byte[] torrentBytes = torrent.toJSONString().getBytes();

		// hash and encrypt torrent info. insert into DHT2
		// hash = torrent info + current time
		byte[] timeBytes = new SimpleDateFormat("HH:mm:ss").format(
				Calendar.getInstance().getTime()).getBytes();

		String[] torrentInfo = new String[3];
		
		String hash = Crypto.getMD5Hash(Utils.concat(torrentBytes, timeBytes));
		torrentInfo[0] = hash;

		Object[] encryptionRes = Crypto.encryptAES(torrentBytes, secretKey);
		torrentInfo[1] = (String) encryptionRes[0];
		torrentInfo[2] = (String) encryptionRes[1];
		byte[] encryptedTorrent = (byte[]) encryptionRes[2];

		return new Object[] { torrentInfo, encryptedTorrent };
	}

	// assumes pieces of proper size

	// returns Object[][] where each element represents a piece
	// and each piece is represented as {Key key, byte[] secretKey, byte[]
	// initializationVector}
	private String[][] insertPieces(byte[][] pieces) throws IOException,
			ServiceException, NoSuchAlgorithmException, InvalidKeyException,
			NoSuchPaddingException, InvalidParameterSpecException,
			IllegalBlockSizeException, BadPaddingException {

		String[][] hash_key_ivs = new String[pieces.length][3];

		int i = 0;
		for (byte[] piece : pieces) {
			System.out.println("inserting piece " + i);
			hash_key_ivs[i++] = insertPiece(piece);
		}

		return hash_key_ivs;
	}

	// hashes, encrypts and inserts piece in DHT1
	// returns {Key key, byte[] secretKey, byte[] initializationVector}
	private String[] insertPiece(byte[] data) throws ServiceException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidParameterSpecException,
			IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException {
		String[] hash_key_iv = new String[3];

		// hash data
		String keyString = Crypto.getMD5Hash(data);
		Key key = new Key(keyString);
		hash_key_iv[0] = keyString;

		// encrypt data
		byte[] encryptedData;
		Object[] encResult = Crypto.encryptAES(data);
		hash_key_iv[1] = (String) encResult[0];
		hash_key_iv[2] = (String) encResult[1];
		encryptedData = (byte[]) encResult[2];

		// add data to dht
		this.dht1.insert(key, encryptedData);

		return hash_key_iv;
	}

	private void deletePieces(ArrayList<ArrayList<String>> hash_key_ivs)
			throws ServiceException {
		for (int i = 0; i < hash_key_ivs.size(); i++) {
			ArrayList<String> hash_key_iv = hash_key_ivs.get(i);
			// delete piece from DHT using key
			deletePiece(hash_key_iv);
		}
	}

	private void deletePiece(ArrayList<String> hash_key_iv)
			throws ServiceException {
		Set<Serializable> set = getPiece1(new Key(hash_key_iv.get(0)));
		byte[] pieceBytes = (byte[]) (set.toArray()[0]);
		this.dht1.remove(new Key(hash_key_iv.get(0)), pieceBytes);
	}

	private Set<Serializable> getPiece1(Key key) throws ServiceException {
		return this.dht1.retrieve(key);
	}

	public static ChordWrapper initNetwork(int DHTPort1, int DHTPort2) {
		PropertiesLoader.loadPropertyFile();
		String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

		try {
			System.out.println(Utils.getMyIP());
			URL localURLDHT1 = new URL(PROTOCOL + "://"
					+ Utils.getMyIP() + ":"
					+ DHTPort1 + "/");
			
			URL localURLDHT2 = new URL(PROTOCOL + "://"
					+ Utils.getMyIP() + ":"
					+ DHTPort2 + "/");

			return new ChordWrapper(localURLDHT1, localURLDHT2, "initPeer");

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	public static ChordWrapper joinNetwork(int DHTPort1, int DHTPort2,
			String bootstrapDHT1, String bootstrapDHT2) {
		PropertiesLoader.loadPropertyFile();
		String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		try {
			URL localURLDHT1 = new URL(PROTOCOL + "://"
					+ Utils.getMyIP() + ":"
					+ DHTPort1 + "/");

			URL localURLDHT2 = new URL(PROTOCOL + "://"
					+ Utils.getMyIP() + ":"
					+ DHTPort2 + "/");

			URL bootstrappedDHT1 = new URL(PROTOCOL + "://" + bootstrapDHT1
					+ "/");

			URL bootstrappedDHT2 = new URL(PROTOCOL + "://" + bootstrapDHT2
					+ "/");

			return new ChordWrapper(localURLDHT1, localURLDHT2,
					bootstrappedDHT1, bootstrappedDHT2, "bootstrapped");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) {

		// initialize network
		ChordWrapper init = ChordWrapper.initNetwork(8000, 4000);

		// joinExistingNetwork starts
		// PropertiesLoader.loadPropertyFile();

		// ChordWrapper bootstrapper = ChordWrapper.joinNetwork(8001, 4001,
		// "192.168.1.1:8000", "192.168.1.1:4000");

		String[] torrentinfo = null;

		Scanner sc = new Scanner(System.in);
		sc.nextLine();
		try {
			torrentinfo = init.uploadFile("IMG_8840.JPG");
			// the bootstrapper should know the torrentinfo somehow
			// use sockets for testing sake
			// DatagramSocket datagramSocket = new DatagramSocket(null);
			// datagramSocket.bind(new
			// InetSocketAddress(InetAddress.getByName(""),5000));

			// for(int i=0;i<torrentinfo.length;i++){
			// System.out.println(torrentinfo[i]);
			// DatagramPacket packet = new DatagramPacket(message,
			// message.length, address, PORT); // create packet to send
			// datagramSocket.send(packet);
			// }

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			// bootstrapper.downloadFile(torrentinfo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// will be called when the peer receives something
	public void receivedBytes(byte[] bs) {
	}
	
	// call to send bytes to a peer
	public void sendBytes(byte[] bs, String ip, int port) {
		this.networking.sendBytes(bs, ip, port);
	}
}
