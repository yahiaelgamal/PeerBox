package PeerBox;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.simple.parser.ParseException;

import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class ChordWrapper {

	// use over real network
//	static String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

	// use for testing on the JVM/thread
	 static String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);

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

		dht2.insert(new Key(hash), encryptedTorrent);

		return torrentInfo;
	}
	
	public String[] update(String filename, String[] torrentInfo)throws IOException,
	InvalidKeyException, NoSuchAlgorithmException,
	NoSuchPaddingException, InvalidParameterSpecException,
	IllegalBlockSizeException, BadPaddingException, ServiceException, 
	InvalidAlgorithmParameterException, ParseException {

		String[] newtorrentInfo = new String[3];
		byte[] keytemp = Utils.fromHexString(Crypto.generateAESSecret());
		
		//get old torrentinfo
		String hash = torrentInfo[0];
		byte[] key = Utils.fromHexString(torrentInfo[1]);
		byte[] iv = Utils.fromHexString(torrentInfo[2]);
		byte[] torrentBytes = (byte[]) (getPiece2(new Key(hash)).toArray()[0]);
		
		// decrypt torrent info
		byte[] torrentDecrypted = Crypto.decryptAES(torrentBytes, key, iv);

		TorrentConfig torrentJSON = new TorrentConfig(torrentDecrypted);
		String[][] hash_key_ivs = torrentJSON.getAllPiecesInfo();
		
		for (int i = 0; i < hash_key_ivs.length; i++) {

			String[] hash_key_iv = hash_key_ivs[i];
			// get piece from DHT using key
			Set<Serializable> set = getPiece1(new Key(hash_key_iv[0]));
			byte[] pieceBytes = (byte[]) (set.toArray()[0]);
			this.dht1.remove(new Key(hash_key_iv[0]), pieceBytes);
		}
		
		//f(keytemp, keytorr) = keynew
		byte[] keynew = function(keytemp, key);
		
		// divide to pieces, hash, encrypt and insert into DHT1
		byte[][] pieces = fileManager.splitFiles(filename);
		String[][] pieceInfo = insertPieces(pieces);

		// generate torrent info
		TorrentConfig torrent = new TorrentConfig(pieceInfo);
		byte[] newtorrentBytes = torrent.toJSONString().getBytes();

		// hash and encrypt torrent info. insert into DHT2
		// hash = torrent info + current time
		byte[] timeBytes = new SimpleDateFormat("HH:mm:ss").format(
				Calendar.getInstance().getTime()).getBytes();

		String newhash = Crypto.getMD5Hash(Utils.concat(newtorrentBytes, timeBytes));
		newtorrentInfo[0] = newhash;

		Object[] encryptionRes = Crypto.encryptAES(newtorrentBytes, keynew);
		newtorrentInfo[1] = (String) encryptionRes[0];
		newtorrentInfo[2] = (String) encryptionRes[1];
		byte[] encryptedTorrent = (byte[]) encryptionRes[2];

		byte value = 0;
		byte[] temp = {value};
		
		Key key2 = new Key(newhash);
		this.dht2.insert(new Key(hash), Utils.concat(Utils.concat(temp,keytemp), key2.getBytes()));
		value = (byte) 255;
		temp = new byte[]{value};
		this.dht2.insert(key2, Utils.concat(temp,encryptedTorrent));
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
	public void downloadFile(String[] torrentInfo)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException,
			ServiceException, IOException, ParseException {

		// get encrypted torrent info from dht2
		String hash = torrentInfo[0];
		byte[] key = Utils.fromHexString(torrentInfo[1]);
		byte[] iv = Utils.fromHexString(torrentInfo[2]);
		byte[] torrentBytes = (byte[]) (getPiece2(new Key(hash)).toArray()[0]);

		// decrypt torrent info
		byte[] torrentDecrypted = Crypto.decryptAES(torrentBytes, key,
				iv);
		System.out.println("====================");
		System.out.println(torrentDecrypted);
		System.out.println("====================");
		TorrentConfig torrentConfig = new TorrentConfig(torrentDecrypted);
		String filename = (String) torrentConfig.get("filename");
		
		ArrayList<ArrayList<String>> hash_key_ivs = torrentConfig.getAllPiecesInfo();
		System.out.println(hash_key_ivs);

		// download and combine pieces
		downloadFile(filename, hash_key_ivs);
	}

	// gets required pieces from DHT1, decrypts each, and combines them into
	// file with name filename
	private void downloadFile(String filename, ArrayList<ArrayList<String>> hash_key_ivs)
			throws ServiceException, IOException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		FileOutputStream fos = new FileOutputStream(
				fileManager.buildFullPath(filename), true);

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
			decryptedBytes = Crypto.decryptAES(pieceBytes, secretKey,
					iv);

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

	public static void main(String[] args) {
		System.out.println(System.getProperty("java.class.path"));
		int nrPeers = 10;
		try {
			PropertiesLoader.loadPropertyFile();

			URL localURL1 = new URL(PROTOCOL + "://localhost:8000/");
			URL localURL2 = new URL(PROTOCOL + "://localhost:4000/");
			ChordWrapper first = new ChordWrapper(localURL1, localURL2,
					"peer0/");
			System.out.println("Created first peer");

			ChordWrapper[] wrappers = new ChordWrapper[nrPeers];
			wrappers[0] = first;

			for (int i = 1; i < nrPeers; i++) {
				int port1 = 8000 + i;
				int port2 = 4000 + i;

				URL newURL1 = new URL(PROTOCOL + "://localhost:" + port1 + "/");
				URL newURL2 = new URL(PROTOCOL + "://localhost:" + port2 + "/");

				// localURL (URL for someone in the network) will be known by a
				// higher level discovery mechanism
				wrappers[i] = new ChordWrapper(newURL1, newURL2, localURL1,
						localURL2, "peer" + i + "/");
			}

			System.out.println("peer[0] is splitting files");
			String[] torrentInfo = wrappers[0].uploadFile("IMG_8840.JPG");
			System.out.println("peer[0] split ended");

			// assumption of knowing the keys
			// JUST FOR TESTING peer2 will retreive the picture
			System.out.println("Peer 2 is getting peices .. ");
//			wrappers[2].downloadFile("retreivedFile.jpg", torrentInfo);
			wrappers[2].downloadFile(torrentInfo);
			System.out.println("check peer2 folder for a surprise");
			// VOALA WE HAVE A DROPBOX

			// go to console and try retrieving the hashes, it will work !

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
