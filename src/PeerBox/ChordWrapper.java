package PeerBox;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class ChordWrapper {

	// use over real network
	static String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

	// use for testing on the JVM/thread
	// String static PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);

	public Chord chord;
	public FileManager fileManager;

	// In case of a creator
	public ChordWrapper(URL myURL, String myFolder) {
		try {
			this.chord = new ChordImpl();
			this.chord.create(myURL);
			this.fileManager = new FileManager(myFolder);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// In case of bootstraper
	public ChordWrapper(URL myURL, URL bootstrapURL, String myFolder) {
		try {
			this.chord = new ChordImpl();
			this.chord.join(myURL, bootstrapURL);
			this.fileManager = new FileManager(myFolder);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// splits file into pieces, hashes and encrypts each, and inserts them into
	// DHT1
	public String[][] uploadFile(String filename) {
		try {
			byte[][] pieces = fileManager.splitFiles(filename);
			return insertPieces(pieces);

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

	// gets required pieces from DHT1, decrypts each, and combines them into
	// file with name filename
	public void downloadFile(String filename, String[][] hash_key_ivs)
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
			Set<Serializable> set = getPiece(new Key(hash_key_iv[0]));
			pieceBytes = (byte[]) (set.toArray()[0]);

			// secret key and IV
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
	// and each piece is represented as {Key key, byte[] secretKey, byte[] initializationVector}
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
		String secretKeyString = EncryptionUtils.generateAESSecret();
		byte[] secretKeyBytes = EncryptionUtils.fromHexString(secretKeyString);
		SecretKeySpec secretKey = new SecretKeySpec(secretKeyBytes, "AES");
		hash_key_iv[1] = secretKeyString;

		// encrypt data
		byte[] encryptedData;
		Object[] encResult = EncryptionUtils.encryptAES(data, secretKey);
		hash_key_iv[2] = (String) encResult[0];
		encryptedData = (byte []) encResult[1];

		// add data to dht
		this.chord.insert(key, encryptedData);

		return hash_key_iv;
	}

	private Set<Serializable> getPiece(Key key) throws ServiceException {
		return this.chord.retrieve(key);
	}

	public static void main(String[] args) {
		System.out.println(System.getProperty("java.class.path"));
		int nrPeers = 10;
		try {
			PropertiesLoader.loadPropertyFile();

			URL localURL = new URL(PROTOCOL + "://localhost:8000/");
			ChordWrapper first = new ChordWrapper(localURL, "peer0/");
			System.out.println("Created first peer");

			ChordWrapper[] wrappers = new ChordWrapper[nrPeers];
			wrappers[0] = first;

			for (int i = 1; i < nrPeers; i++) {
				int port = 8000 + i;

				URL newURL = new URL(PROTOCOL + "://localhost:" + port + "/");

				// localURL (URL for someone in the network) will be known by a
				// higher level discovery mechanism
				wrappers[i] = new ChordWrapper(newURL, localURL, "peer" + i
						+ "/");
			}

			System.out.println("peer[0] is splitting files");
			String[][] keysAndIVs = wrappers[0].uploadFile("IMG_8840.JPG");
			System.out.println("peer[0] split ended");

			// assumption of knowing the keys
			// JUST FOR TESTING peer2 will retreive the picture
			System.out.println("Peer 2 is getting peices .. ");
			wrappers[2].downloadFile("retreivedFile.jpg", keysAndIVs);
			System.out.println("check peer2 folder for a surprise");
			// VOALA WE HAVE A DROPBOX

			// go to console and try retrieving the hashes, it will work !

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
