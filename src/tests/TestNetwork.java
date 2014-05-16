package tests;

import java.io.ObjectInputStream.GetField;
import java.net.MalformedURLException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONValue;

import PeerBox.ChordWrapper;
import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import networking.ServerClient;

public class TestNetwork {

	public static void testSimpleNetwork() throws Exception{
		// make sure no proxy
		ServerClient sc = new ServerClient(4023, null);
		ServerClient sc2 = new ServerClient(4024, null);
		String s = "aint' nobody got time for dat";
		System.out.println("sending bytes");
		sc.sendBytes(s.getBytes(), "0.0.0.0", 4024);
		System.out.println("Should see: " + "aint' nobody got time for dat");
	}
	
	public static void sendTorrentInfoAndDownload() throws Exception{
		PropertiesLoader.loadPropertyFile();
		URL[] urls0 = TestUtils.makeURLs(0);
		URL[] urls1 = TestUtils.makeURLs(1);
		ChordWrapper peer0  = new ChordWrapper(urls0[0], urls0[1], urls0[2], "peer0");
		
		ChordWrapper peer1  = new ChordWrapper(urls1[0], urls1[1], urls1[2], urls0[0], urls0[1], urls0[2], "peer1");
		
		
		System.out.println("peer0 uploading file");
		String[] torrentInfo = peer0.uploadFile("IMG_8840.JPG");
		
		ArrayList<String> torrentInfoArrayList = PeerBox.Utils
				.convertFromArrayToArrayList(torrentInfo);
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("filename", "IMG_8840.JPG");
		map.put("torrentInfo", torrentInfoArrayList);
		String jsonString = JSONValue.toJSONString(map);
		
		System.out.println("json string is \n" + jsonString);
		System.out.println("there are " + jsonString.getBytes().length);
		peer0.sendBytes(jsonString.getBytes(),
				urls1[0].getHost(), 
				peer1.networking.port);		
		
	}
	public static void testSharing() throws Exception{
		PropertiesLoader.loadPropertyFile();
		URL[] urls0 = TestUtils.makeURLs(0);
		URL[] urls1 = TestUtils.makeURLs(1);
		URL[] urls2 = TestUtils.makeURLs(2);

		ChordWrapper peer0  = new ChordWrapper(urls0[0], urls0[1], urls0[2], "peer0");
		ChordWrapper peer1  = new ChordWrapper(urls1[0], urls1[1], urls1[2], 
				urls0[0], urls0[1], urls0[2], "peer1");
		ChordWrapper peer2  = new ChordWrapper(urls2[0], urls2[1], urls2[2], 
				urls0[0], urls0[1], urls0[2], "peer2");
		System.out.println("peer0 sharing file");
		String[] torrentInfo = peer0.uploadFile("IMG_8840.JPG");
		
		peer0.shareFile(torrentInfo[0], torrentInfo[1], torrentInfo[2], 
				"IMG_8840.JPG", "10-40-F3-A7-CD-90");
		
		
	}
	
	public static void main(String[] args) throws Exception{
		testSharing();
	}
}
