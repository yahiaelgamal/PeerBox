package PeerBox;

import java.util.ArrayList;

import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class Utils {

	public static ArrayList convertFromArrayToArrayList(Object[] arr) {
		ArrayList list = new ArrayList();
		for(Object obj : arr) {
			list.add(obj);
		}
		return list;
	}
	// encodes bytes as hexadecimal string
	public static String toHexString(byte[] bytes) {
		String string = "";
		for (int i = 0; i < bytes.length; i++) {
			if ((0xff & bytes[i]) < 0x10) {
				string += "0" + Integer.toHexString((0xFF & bytes[i]));
			} else {
				string += Integer.toHexString(0xFF & bytes[i]);
			}
		}
		return string;
	}

	// decodes hexadecimal string back to bytes
	public static byte[] fromHexString(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static byte[] concat(byte[] A, byte[] B) {
		int aLen = A.length;
		int bLen = B.length;
		byte[] C = new byte[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}
	
	public static boolean isTorrentFile(byte[] data) {
		if (((int)data[0]) == 0) {
			return false;
		} else {
			return true;
		}
	}
}
