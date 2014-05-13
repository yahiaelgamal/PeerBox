package tests;

import networking.ServerClient;

public class TestNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// make sure no proxy
		ServerClient sc = new ServerClient(4023, null);
		ServerClient sc2 = new ServerClient(4024, null);
		String s = "aint' nobody got time for dat";
		System.out.println("sending bytes");
		sc.sendBytes(s.getBytes(), "0.0.0.0", 4024);
	}
}
