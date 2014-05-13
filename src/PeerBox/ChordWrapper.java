package PeerBox;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Set;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class ChordWrapper {

	// use over real network
	static String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
	PublicKey publicKey;
	PrivateKey privateKey;
	// use for testing on the JVM/thread
	// String static PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);

	public Chord dht1;
	public Chord dht2;
	public Chord dht3;
	public FileManager fileManager;

	// In case of a creator
	public ChordWrapper(URL myURL1, URL myURL2, URL myURL3, 
			String myFolder, PublicKey publicKey, PrivateKey privateKey) {
		try {
			this.dht1 = new ChordImpl();
			this.dht1.create(myURL1);

			this.dht2 = new ChordImpl();
			this.dht2.create(myURL2);

			this.dht3 = new ChordImpl();
			this.dht3.create(myURL3);
			
			this.fileManager = new FileManager(myFolder);
			
		    this.publicKey = publicKey;
		    this.privateKey = privateKey;
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// In case of bootstraper
	public ChordWrapper(URL myURL1, URL myURL2, URL myURL3, URL bootstrapURL1,
			URL bootstrapURL2, URL bootstrapURL3, String myFolder) {
		try {
			this.dht1 = new ChordImpl();
			this.dht1.join(myURL1, bootstrapURL1);

			this.dht2 = new ChordImpl();
			this.dht2.join(myURL2, bootstrapURL2);

				
			this.dht3 = new ChordImpl();
			this.dht3.join(myURL3, bootstrapURL3);
			
			this.fileManager = new FileManager(myFolder);
			//get macAddress
			try
			{
				InetAddress address = InetAddress.getLocalHost();
				NetworkInterface nwi = NetworkInterface.getByInetAddress(address);
				byte mac[] = nwi.getHardwareAddress();
				if(mac != null) {
					StringBuilder macAddress = new StringBuilder();
					for (int i = 0; i < mac.length; i++) {
						macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
					}
					Set<Serializable> PK_receiver = getPiece3(new Key(macAddress.toString()));	
					if(PK_receiver == null){
						KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
					    keyGen.initialize(1024);
					    KeyPair key = keyGen.generateKeyPair();
					    PublicKey publicKey = key.getPublic();
					    PrivateKey privateKey = key.getPrivate();
					    dht3.insert(new Key(macAddress.toString()), publicKey);
					    
					}
				}
			}
			catch(Exception e)
			{
				System.out.println("ERROR");
			}
//			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
//		    keyGen.initialize(1024);
//		    KeyPair key = keyGen.generateKeyPair();
//		    PublicKey publicKey = key.getPublic();
//		    PrivateKey privateKey = key.getPrivate();
//			
			this.publicKey = publicKey;
			this.privateKey = privateKey;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// splits file into pieces, hashes and encrypts each, and inserts them into
	// DHT1. generates torrent JSON string, hashes and encrypts it and inserts
	// it into DHT2
	// returns torrent info in the form
	// {String torrentHash, String torrentKey, String iv}
	public String[] uploadFile(String filename) {
		try {
			String[] torrentInfo = new String[3];

			// divide to pieces, hash, encrypt and insert into DHT1
			byte[][] pieces = fileManager.splitFiles(filename);
			String[][] pieceInfo = insertPieces(pieces);

			// generate torrent info
			TorrentConfig torrent = new TorrentConfig(pieceInfo);
			byte[] torrentJSON = torrent.toJSONString().getBytes();

			// hash and encrypt torrent info. insert into DHT2
			String hash = EncryptionUtils.getMD5Hash(torrentJSON);
			torrentInfo[0] = hash;

			Object[] encryptionRes = EncryptionUtils.encryptAES(torrentJSON);
			torrentInfo[1] = (String) encryptionRes[0];
			torrentInfo[2] = (String) encryptionRes[1];
			byte[] encryptedTorrent = (byte[]) encryptionRes[2];

			dht2.insert(new Key(hash), encryptedTorrent);

			return torrentInfo;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidParameterSpecException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void downloadFile(String filename, String[] torrentInfo)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException,
			ServiceException, IOException, ParseException {
		
		// get encrypted torrent info from dht2
		String hash = torrentInfo[0];
		byte[] key = EncryptionUtils.fromHexString(torrentInfo[1]);
		byte[] iv = EncryptionUtils.fromHexString(torrentInfo[2]);
		byte[] torrentBytes = (byte[]) (getPiece2(new Key(hash)).toArray()[0]);
		
		// decrypt torrent info
		byte[] torrentDecrypted = EncryptionUtils.decryptAES(torrentBytes, key, iv);
		
		TorrentConfig torrentJSON = new TorrentConfig(torrentDecrypted);
		String[][] hash_key_ivs = torrentJSON.getAllPiecesInfo();

		// download and combine pieces
		downloadFile(filename, hash_key_ivs);
	}

	// gets required pieces from DHT1, decrypts each, and combines them into
	// file with name filename
	private void downloadFile(String filename, String[][] hash_key_ivs)
			throws ServiceException, IOException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		FileOutputStream fos = new FileOutputStream(
				fileManager.buildFullPath(filename), true);

		byte[] pieceBytes, decryptedBytes;
		for (int i = 0; i < hash_key_ivs.length; i++) {
			System.out.println("Getting piece " + i);

			String[] hash_key_iv = hash_key_ivs[i];

			// get piece from DHT using key
			Set<Serializable> set = getPiece1(new Key(hash_key_iv[0]));
			pieceBytes = (byte[]) (set.toArray()[0]);

			// secret key and IV
			// consider changing! we convert byte[] to string and back to byte[]
			byte[] secretKey = EncryptionUtils.fromHexString(hash_key_iv[1]);
			byte[] iv = EncryptionUtils.fromHexString(hash_key_iv[2]);

			// decrypt piece
			decryptedBytes = EncryptionUtils.decryptAES(pieceBytes, secretKey,
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
		String keyString = EncryptionUtils.getMD5Hash(data);
		Key key = new Key(keyString);
		hash_key_iv[0] = keyString;

		// generate secret key

		// encrypt data
		byte[] encryptedData;
		Object[] encResult = EncryptionUtils.encryptAES(data);
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
	
	private Set<Serializable> getPiece3(Key key) throws ServiceException {
		return this.dht3.retrieve(key);
	}
	
	// Sends torrentHash, torrentKey and IV encrypted using PK_receiver
	// Assumes DHT3 with mapping receiverUniqueAddress -> PK_receiver
	// Tries for sending directly to receiver, and if fails sends to 
	// bootstrapping DNS which will keep them for forwarding
	public boolean shareFile(String torrentHash, String torrentKey,
			String IV, String receiverUniqueAddress) {
		try {
			//prepare 
			Set<Serializable> PK_receiver = getPiece3(new Key(receiverUniqueAddress));			
			JSONObject data = new JSONObject();
			data.put("torrentHash", torrentHash);
			data.put("torrentKey", torrentKey);
			data.put("IV", IV);
			byte[] dataBytes = data.toJSONString().getBytes();
			
			byte[] encrypted = EncryptionUtils.encryptWithPK(
					(PublicKey)PK_receiver.toArray()[0], dataBytes);
			
			
			
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public static void main(String[] args) {

		System.out.println(System.getProperty("java.class.path"));
		int nrPeers = 10;
		try {
			
			PropertiesLoader.loadPropertyFile();
			
			URL localURL1 = new URL(PROTOCOL + "://localhost:4000/");
			URL localURL2 = new URL(PROTOCOL + "://localhost:6000/");
			URL localURL3 = new URL(PROTOCOL + "://localhost:8000/");
			
			// create public key
		    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		    keyGen.initialize(1024);
		    KeyPair key = keyGen.generateKeyPair();
		    PublicKey publicKey = key.getPublic();
		    PrivateKey privateKey = key.getPrivate();
		    
		    
			ChordWrapper first = new ChordWrapper(localURL1, localURL2, 
					localURL3, "peer0/", publicKey, privateKey);
			System.out.println("Created first peer");

			ChordWrapper[] wrappers = new ChordWrapper[nrPeers];
			wrappers[0] = first;

			for (int i = 1; i < nrPeers; i++) {
				int port1 = 4000 + i;
				int port2 = 6000 + i;
				int port3 = 8000 + i;
				
				URL newURL1 = new URL(PROTOCOL + "://localhost:" + port1 + "/");
				URL newURL2 = new URL(PROTOCOL + "://localhost:" + port2 + "/");
				URL newURL3 = new URL(PROTOCOL + "://localhost:" + port3 + "/");

				key = keyGen.generateKeyPair();
			    publicKey = key.getPublic();
			    privateKey = key.getPrivate();
				// localURL (URL for someone in the network) will be known by a
				// higher level discovery mechanism
				wrappers[i] = new ChordWrapper(newURL1, newURL2, newURL3, localURL1,
						localURL2, localURL3, "peer" + i + "/");
			}

			System.out.println("peer[0] is splitting files");
			String[] torrentInfo = wrappers[0].uploadFile("IMG_8840.JPG");
			System.out.println("peer[0] split ended");

			// assumption of knowing the keys
			// JUST FOR TESTING peer2 will retreive the picture
			System.out.println("Peer 2 is getting peices .. ");
			wrappers[2].downloadFile("retreivedFile.jpg", torrentInfo);
			System.out.println("check peer2 folder for a surprise");
			// VOALA WE HAVE A DROPBOX

			// go to console and try retrieving the hashes, it will work !

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}