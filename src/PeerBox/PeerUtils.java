package PeerBox;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class PeerUtils {
	
	// creates byte[] of the form <key1, 0x00 || tempKey || key2 || iv2>
	public static byte[] createDHT2Entry1(byte[] tempKey, byte[] hash2Bytes, byte[] iv2) {
		return Utils.concat(Utils.concat(Utils.concat(new byte[] { (byte) 0x00 }, 
				tempKey),
				hash2Bytes), 
				iv2);
	}

	// creates byte[] of the form <key2, 0xff || encTorrent>
	public static byte[] createDHT2Entry2(byte[] encryptedTorrent) {
		return Utils.concat(new byte[] { (byte) 0xff }, encryptedTorrent);
	}

	// splits byte[] into {{0x00}, {tempKey}, {key2}, {iv2}}
	public static byte[][] splitDHTEntry1(byte[] dhtEntry)
			throws NoSuchAlgorithmException {

		byte[] opcode = Arrays.copyOfRange(dhtEntry, 0, 1);
		byte[] tempKey = Arrays.copyOfRange(dhtEntry, 1, Crypto.SECRET_KEY_LEN + 1);
		byte[] key2 = Arrays.copyOfRange(dhtEntry, Crypto.SECRET_KEY_LEN + 1,
				Crypto.SECRET_KEY_LEN + Crypto.getDigestLength() + 1);
		byte[] iv2 = Arrays.copyOfRange(dhtEntry, 
				Crypto.SECRET_KEY_LEN + Crypto.getDigestLength() + 1, 
				dhtEntry.length);

		return new byte[][] { opcode, tempKey, key2, iv2 };
	}

	// splits byte[] into {{0xff}, {torrentString}}
	public static byte[][] splitDHTEntry2(byte[] dhtEntry) {
		byte[] opcode = Arrays.copyOfRange(dhtEntry, 0, 1);
		byte[] torrentString = Arrays.copyOfRange(dhtEntry, 1, dhtEntry.length);

		return new byte[][] { opcode, torrentString };
	}

	public static byte[] function(byte[] keytemp, byte[] key) {
		byte[] out = new byte[keytemp.length];
		int i = 0;
		for (byte b : keytemp)
			out[i] = (byte) (b ^ key[i++]);
		return out;
	}
}
