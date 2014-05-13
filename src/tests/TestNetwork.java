package tests;

import networking.ServerClient;

public class TestNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		ServerClient sc = new ServerClient(4023);
		ServerClient sc2 = new ServerClient(4024);
		String s = "aint' nobody got time for dat";
		System.out.println("sending bytes");
		sc.sendBytes(s.getBytes(), "0.0.0.0", 4024);
	}

}
