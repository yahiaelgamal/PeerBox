package PeerBox;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

public class Utils {

	public static String getMyIP() {
		final String IP_REGEX = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

		String ip;
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				// filters out 127.0.0.1
				if (iface.isLoopback() || !iface.isUp())
					continue;

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					ip = addr.getHostAddress();

					if (ip.matches(IP_REGEX) && !ip.equals("127.0.1.1")) {
						System.out.println(ip);
						return ip;
					}

				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	public static ArrayList convertFromArrayToArrayList(Object[] arr) {
		ArrayList list = new ArrayList();
		for (Object obj : arr) {
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
		if ((data[0]) == 0) {
			return false;
		} else {
			return true;
		}
	}
}
