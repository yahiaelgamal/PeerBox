package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

import PeerBox.ChordWrapper;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;

public class UpdateTest {

	public static URL[] makeURLs(int i){
		try {
			URL localURL0 = new URL(ChordWrapper.PROTOCOL + "://localhost:" + (4000+i) + "/");
			URL localURL1 = new URL(ChordWrapper.PROTOCOL + "://localhost:" + (5000+i) + "/");
			URL localURL2 = new URL(ChordWrapper.PROTOCOL + "://localhost:" + (6000+i) + "/");
			return new URL[] {localURL0, localURL1, localURL2};
		}catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(3);
			return null;
		}
	}
	
	public static void testSocket() {
		PropertiesLoader.loadPropertyFile();
		// initialize network
//		ChordWrapper init = ChordWrapper.initNetwork(8000, 4000);

		URL[] initURLs = makeURLs(0);
		System.out.println(Arrays.toString(initURLs));
		URL[] bootstrappedURLs = makeURLs(1);
		System.out.println(Arrays.toString(bootstrappedURLs));
		
		ChordWrapper init = new ChordWrapper(initURLs[0], initURLs[1], "init");
		ChordWrapper bootstrapper = new ChordWrapper(bootstrappedURLs[0],
				bootstrappedURLs[1], initURLs[0], initURLs[1], "bootstraped");
		

		// joinExistingNetwork starts

		String[] torrentinfo = null;

		Scanner sc = new Scanner(System.in);
		System.out.println("Press enter to upload");
		sc.nextLine();
		try {
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		    
	}
	public static void testUpdate() {

		
		try {

			PropertiesLoader.loadPropertyFile();
			URL[] bootsrap = makeURLs(0);
			
			System.out.println(Arrays.toString(bootsrap));
			ChordWrapper peer0 = new ChordWrapper(bootsrap[0], bootsrap[1], "owner/");
			
			URL[] urls = makeURLs(1);
			ChordWrapper peer1 = new ChordWrapper(urls[0], urls[1], bootsrap[0], bootsrap[1], "peer1/");
			
			urls = makeURLs(2);
			ChordWrapper peer2 = new ChordWrapper(urls[0], urls[1], bootsrap[0], bootsrap[1], "peer2/");
			
			System.out.println("created 3 peers");
			
			String s = "File version 1";
			peer0.fileManager.writeToRelativeFile("coolFile.txt", s.getBytes());
		
			System.out.println("Peer 0 uploading coolfile.txt");
			String[] torrentInfo = peer0.uploadFile("coolFile.txt");
			
			peer1.downloadFile(torrentInfo);
			peer2.downloadFile(torrentInfo);
			
			
			peer1.fileManager.writeToRelativeFile("coolFile.txt", "file versino 2".getBytes());
			System.out.println("Peer 1 edited file");
			
			
			String[] torrentInfo2 = peer1.update("coolFile.txt", torrentInfo);
			System.out.println(Arrays.toString(torrentInfo2));
			
			// assume
//			peer2.downloadFile(torrentInfo2);
			peer2.sync("coolFile.txt");
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) {
//		testUpdate();
		testSocket();
	}
}
