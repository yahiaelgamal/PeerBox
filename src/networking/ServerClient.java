package networking;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import PeerBox.ChordWrapper;


public class ServerClient {

	
	ServerSocket listener;
	ChordWrapper chordWrapper;
	public ServerClient(int port, ChordWrapper owner) throws IOException{
		this.listener = new ServerSocket(port);
		this.chordWrapper = owner;
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
				// TODO dispatch other threads
					while(true) {
						Socket socket = listener.accept();
	                	System.out.println("I got this ");
	                	byte[] bs = new byte[1024];
	                	socket.getInputStream().read(bs);
	                	
	                	// for testing 
	                	if(chordWrapper != null)
		                	chordWrapper.receivedBytes(bs);
	                	else
	                		System.out.println("chord wrapper is null");
	                	System.out.println("received " + new String(bs));
					}
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		};
		System.out.println("Running .. ");
		Thread t = new Thread(r);
		t.start();
		System.out.println("SErver rning on port " + port);
	}
	
	public boolean sendBytes(byte[] bytes, String ip, int port){
		System.out.println("Sending ...");
		try {
	        Socket s;
			s = new Socket(ip, port);
	        OutputStream os = s.getOutputStream();
	        os.write(bytes);
	        os.close();
	        s.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
