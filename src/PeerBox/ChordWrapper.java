package PeerBox;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.rmi.CORBA.Util;
import javax.swing.plaf.SliderUI;

import org.json.simple.parser.ParseException;

import de.uniba.wiai.lspi.chord.console.command.Wait;
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

	public Chord dht1;
	public Chord dht2;
	public FileManager fileManager;

	// In case of a creator
	public ChordWrapper(URL myURL1, URL myURL2, String myFolder) {
		try {
			this.dht1 = new ChordImpl();
			this.dht1.create(myURL1);

			this.dht2 = new ChordImpl();
			this.dht2.create(myURL2);

			this.fileManager = new FileManager(myFolder);
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

		String[] torrentInfo = new String[3];

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

		String hash = Crypto.getMD5Hash(Utils.concat(torrentBytes, timeBytes));
		torrentInfo[0] = hash;

		Object[] encryptionRes = Crypto.encryptAES(torrentBytes);
		torrentInfo[1] = (String) encryptionRes[0];
		torrentInfo[2] = (String) encryptionRes[1];
		byte[] encryptedTorrent = (byte[]) encryptionRes[2];

		dht2.insert(new Key(hash),
				Utils.concat(new byte[] { (byte) 255 }, encryptedTorrent));

		return torrentInfo;
	}

	public String[] update(String filename, String[] torrentInfo)
			throws IOException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidParameterSpecException,
			IllegalBlockSizeException, BadPaddingException, ServiceException,
			InvalidAlgorithmParameterException, ParseException {

		String[] newtorrentInfo = new String[3];
		byte[] keytemp = Utils.fromHexString(Crypto.generateAESSecret());

		// get old torrentinfo
		String hash = torrentInfo[0];
		byte[] key = Utils.fromHexString(torrentInfo[1]);
		byte[] iv = Utils.fromHexString(torrentInfo[2]);

		byte[] dhtEntry = (byte[]) (dht2.retrieve(new Key(hash)).toArray()[0]);
		byte[] torrentBytes = Arrays.copyOfRange(dhtEntry, 1, dhtEntry.length);

		// decrypt torrent info
		byte[] torrentDecrypted = Crypto.decryptAES(torrentBytes, key, iv);

		TorrentConfig torrentJSON = new TorrentConfig(torrentDecrypted);
		ArrayList<ArrayList<String>> hash_key_ivs = torrentJSON
				.getAllPiecesInfo();

		for (int i = 0; i < hash_key_ivs.size(); i++) {

			ArrayList<String> hash_key_iv = hash_key_ivs.get(i);
			// get piece from DHT using key
			Set<Serializable> set = getPiece1(new Key(hash_key_iv.get(i)));
			byte[] pieceBytes = (byte[]) (set.toArray()[0]);
			this.dht1.remove(new Key(hash_key_iv.get(i)), pieceBytes);
		}

		// f(keytemp, keytorr) = keynew
		byte[] keynew = function(keytemp, key);

		// divide to pieces, hash, encrypt and insert into DHT1
		byte[][] pieces = fileManager.splitFiles(filename);
		String[][] pieceInfo = insertPieces(pieces);

		// generate torrent info
		TorrentConfig torrent = new TorrentConfig(filename, pieceInfo);
		byte[] newtorrentBytes = torrent.toJSONString().getBytes();

		// hash and encrypt torrent info. insert into DHT2
		// hash = torrent info + current time
		byte[] timeBytes = new SimpleDateFormat("HH:mm:ss").format(
				Calendar.getInstance().getTime()).getBytes();

		String newhash = Crypto.getMD5Hash(Utils.concat(newtorrentBytes,
				timeBytes));
		newtorrentInfo[0] = newhash;

		Object[] encryptionRes = Crypto.encryptAES(newtorrentBytes, keynew);
		newtorrentInfo[1] = (String) encryptionRes[0];
		newtorrentInfo[2] = (String) encryptionRes[1];
		byte[] encryptedTorrent = (byte[]) encryptionRes[2];

		byte value = 0;
		byte[] temp = { value };

		Key key2 = new Key(newhash);
		this.dht2.insert(new Key(hash),
				Utils.concat(Utils.concat(temp, keytemp), key2.getBytes()));
		value = (byte) 255;
		temp = new byte[] { value };
		this.dht2.insert(key2, Utils.concat(temp, encryptedTorrent));
		return newtorrentInfo;
	}

	private byte[] function(byte[] keytemp, byte[] key) {
		byte[] out = new byte[keytemp.length];
		int i = 0;
		for (byte b : keytemp)
			out[i] = (byte) (b ^ key[i++]);
		return out;
	}

	// naive download. assumes file has not been edited.
	// downloads file given its torrent info (hash, key and iv)
	public void downloadFile(String[] torrentInfo) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, ServiceException, IOException,
			ParseException {

		// get encrypted torrent info from dht2
		String hash = torrentInfo[0];
		byte[] key = Utils.fromHexString(torrentInfo[1]);
		byte[] iv = Utils.fromHexString(torrentInfo[2]);
		byte[] dhtEntry = (byte[]) dht2.retrieve(new Key(hash)).toArray()[0];
		byte[] torrentBytes = Arrays.copyOfRange(dhtEntry, 1, dhtEntry.length);

		// decrypt torrent info
		byte[] torrentDecrypted = Crypto.decryptAES(torrentBytes, key, iv);
		System.out.println("====================");
		System.out.println(torrentDecrypted);
		System.out.println("====================");
		TorrentConfig torrentConfig = new TorrentConfig(torrentDecrypted);
		String filename = (String) torrentConfig.get("filename");

		ArrayList<ArrayList<String>> hash_key_ivs = torrentConfig
				.getAllPiecesInfo();
		System.out.println(hash_key_ivs);

		// download and combine pieces
		downloadFile(filename, hash_key_ivs);
	}

	public void sync(String filename) throws ServiceException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException,
			IOException, ParseException {
		// TODO delete old file.

		// get hash, secretKey, iv
		String[] torrentInfo = fileManager.getTorrentInfo(filename);

		// download file
		sync(filename, torrentInfo);
	}

	public static byte[] magicFunction(byte[] tempKey, byte[] key) {
		return null;
	}

	private void sync(String filename, String[] torrentInfo)
			throws ServiceException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, IOException, ParseException {
		String hash = torrentInfo[0];
		byte[] key = Utils.fromHexString(torrentInfo[1]);
		String iv = torrentInfo[2];

		byte[] dht2Entry = (byte[]) dht2.retrieve(new Key(hash)).toArray()[0];

		if (Utils.isTorrentFile(dht2Entry)) {
			downloadFile(torrentInfo);
		} else {
			byte[] tempKey = Arrays.copyOfRange(dht2Entry, 1,
					Crypto.SECRET_KEY_LEN);
			byte[] key2 = Arrays.copyOfRange(dht2Entry,
					Crypto.SECRET_KEY_LEN + 1,
					Crypto.SECRET_KEY_LEN + Crypto.getDigestLength());

			String[] newTorrentInfo = new String[3];
			newTorrentInfo[0] = Utils.toHexString(key2);
			newTorrentInfo[1] = Utils.toHexString(magicFunction(tempKey, key));
			newTorrentInfo[2] = iv;

			fileManager.replaceEntry(filename, newTorrentInfo);
			sync(filename, newTorrentInfo);
		}
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

		// generate secret key

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

	private Set<Serializable> getPiece1(Key key) throws ServiceException {
		return this.dht1.retrieve(key);
	}

	private Set<Serializable> getPiece2(Key key) throws ServiceException {
		return this.dht2.retrieve(key);
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
}
