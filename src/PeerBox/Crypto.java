package PeerBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAPrivateKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

	public static final String HASH_ALGO = "MD5";

	public static final String ENCRYPTION_ALGO = "AES";
	public static final String ENCRYPTION_CONFIG = "AES/CBC/PKCS5Padding";
	public static final int SECRET_KEY_LEN = 128 / 8;

	// generates a 128-bit AES key
	public static byte[] generateAESSecret() throws NoSuchAlgorithmException {
		KeyGenerator kgen = KeyGenerator.getInstance(ENCRYPTION_ALGO);
		kgen.init(SECRET_KEY_LEN * 8);
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();

		return raw;
	}

	// returns MD5 digest of given data as a string
	public static String getMD5Hash(byte[] data)
			throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance(HASH_ALGO);
		byte[] hash = md5.digest(data);

		return Utils.toHexString(hash);
	}

	// encrypts data using AES with key secretKey
	// returns Object[]
	// {String secretKey, String initializationVector, byte[] encryptedData}
	public static Object[] encryptAES(byte[] data)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, InvalidParameterSpecException {

		byte[] secretKeyBytes = generateAESSecret();
		SecretKeySpec secretKey = new SecretKeySpec(secretKeyBytes,
				ENCRYPTION_ALGO);

		Cipher cipher = Cipher.getInstance(ENCRYPTION_CONFIG);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		byte[] iv = cipher.getParameters()
				.getParameterSpec(IvParameterSpec.class).getIV();
		// System.out.println("iv:	 	" + toHexString(iv));

		byte[] encryptedData = cipher.doFinal(data);

		return new Object[] { Utils.toHexString(secretKeyBytes),
				Utils.toHexString(iv), encryptedData };
	}

	// encrypts data using AES with key
	// returns Object[]
	// {String secretKey, String initializationVector, byte[] encryptedData}
	public static Object[] encryptAES(byte[] data, byte[] key)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, InvalidParameterSpecException {

		// String secretKeyString = generateAESSecret();
		// byte[] secretKeyBytes = Utils.fromHexString(secretKeyString);
		SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		byte[] iv = cipher.getParameters()
				.getParameterSpec(IvParameterSpec.class).getIV();
		// System.out.println("iv:	 	" + toHexString(iv));

		byte[] encryptedData = cipher.doFinal(data);

		return new Object[] { Utils.toHexString(key), Utils.toHexString(iv),
				encryptedData };
	}

	// decrypts data using AES given key secretKey an initialization vector iv
	public static byte[] decryptAES(byte[] data, byte[] secretKey, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		SecretKeySpec secret = new SecretKeySpec(secretKey, ENCRYPTION_ALGO);
		Cipher cipher = Cipher.getInstance(ENCRYPTION_CONFIG);
		cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
		byte[] decryptedBytes = cipher.doFinal(data);
		return decryptedBytes;
	}

	public static int getDigestLength() throws NoSuchAlgorithmException {
		return MessageDigest.getInstance(HASH_ALGO).getDigestLength();
	}
	public static byte[] decryptWithPK(PrivateKey pk, byte[] data){
		
		return null;
	}
	 /** 
	  * ready code
	  */  
	public PrivateKey readPrivateKeyFromFile() throws IOException{  
		String fileName = "private_key.txt";
		FileInputStream fis = null;  
		ObjectInputStream ois = null;  
		try {  
			fis = new FileInputStream(new File(fileName));  
			ois = new ObjectInputStream(fis);  

			BigInteger modulus = (BigInteger) ois.readObject();  
			BigInteger exponent = (BigInteger) ois.readObject();  

			//Get Private Key  
			RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);  
			KeyFactory fact = KeyFactory.getInstance("RSA");  
			PrivateKey privateKey = fact.generatePrivate(rsaPrivateKeySpec);  

			return privateKey;  

		} catch (Exception e) {  
			e.printStackTrace();  
		}  
		finally{  
			if(ois != null){  
				ois.close();  
				if(fis != null){  
					fis.close();  
				}  
			}  
		}  
		return null;  
	}   
	public static byte[] encryptWithPK(PublicKey pk, byte[] data){
		try {
			// create RSA public key cipher
			Cipher pkCipher = Cipher.getInstance("RSA");
			pkCipher.init(Cipher.ENCRYPT_MODE, pk);  

			//encode
			byte[] encrypted = pkCipher.doFinal(data);
			return encrypted;

		} catch (InvalidKeyException e) {

			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {

			e.printStackTrace();
		} catch (BadPaddingException e) {

			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	public static byte[] decryptWithPK(PrivateKey key){
		
		return null;
	}
	
}
