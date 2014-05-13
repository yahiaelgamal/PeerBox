package tests;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import PeerBox.ChordWrapper;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;

public class UpdateTest {

	public static URL[] makeURLs(int i) throws MalformedURLException {
		URL localURL0 = new URL(ChordWrapper.PROTOCOL + "://localhost:" + (4000+i) + "/");
		URL localURL1 = new URL(ChordWrapper.PROTOCOL + "://localhost:" + (5000+i) + "/");
		URL localURL2 = new URL(ChordWrapper.PROTOCOL + "://localhost:" + (6000+i) + "/");
		return new URL[] {localURL0, localURL1, localURL2};
		
	}
	
	// scenario:
	// 1. peer0 uploads coolFile.txt that contains the text "file version 1"
	// 2. peer1 and peer2 download file
	// 3. peer1 edits the file to contain the text "file version 2"
	// 4. peer2 syncs the file
	// expected output: peer0 has version 1 and peer 1 and 2 have version 2 
	// assumption: peers 1 and 2 know the old torrent info
	public static void test1() {
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
			
			
			peer1.fileManager.writeToRelativeFile("coolFile.txt", "File version 2".getBytes());
			System.out.println("Peer 1 edited file");
			
			
			String[] torrentInfo2 = peer1.update("coolFile.txt", torrentInfo);
			System.out.println("New Torrent info: " + Arrays.toString(torrentInfo2));
			
			// assume
//			peer2.downloadFile(torrentInfo2);
			peer2.sync("coolFile.txt");
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		test1();
	}
}
