package PeerBox;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

	// generates a 128-bit AES key
	private static String generateAESSecret() throws NoSuchAlgorithmException {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128);
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();

		return Utils.toHexString(raw);
	}

	// returns MD5 digest of given data as a string
	public static String getMD5Hash(byte[] data)
			throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
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

		String secretKeyString = generateAESSecret();
		byte[] secretKeyBytes = Utils.fromHexString(secretKeyString);
		SecretKeySpec secretKey = new SecretKeySpec(secretKeyBytes, "AES");
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		byte[] iv = cipher.getParameters()
				.getParameterSpec(IvParameterSpec.class).getIV();
		// System.out.println("iv:	 	" + toHexString(iv));

		byte[] encryptedData = cipher.doFinal(data);

		return new Object[] { Utils.toHexString(secretKeyBytes), Utils.toHexString(iv), encryptedData };
	}

	// decrypts data using AES given key secretKey an initialization vector iv
	public static byte[] decryptAES(byte[] data, byte[] secretKey, byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		SecretKeySpec secret = new SecretKeySpec(secretKey, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
		byte[] decryptedBytes = cipher.doFinal(data);
		return decryptedBytes;
	}
}
