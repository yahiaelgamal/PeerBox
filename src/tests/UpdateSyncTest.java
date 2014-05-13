package tests;

import java.net.MalformedURLException;
import java.util.Arrays;

import PeerBox.ChordWrapper;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;

public class UpdateSyncTest {

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
			URL[] bootsrap = TestUtils.makeURLs(0);

			System.out.println(Arrays.toString(bootsrap));
			ChordWrapper peer0 = new ChordWrapper(bootsrap[0], bootsrap[1],
					"owner/");

			URL[] urls = TestUtils.makeURLs(1);
			ChordWrapper peer1 = new ChordWrapper(urls[0], urls[1],
					bootsrap[0], bootsrap[1], "peer1/");

			urls = TestUtils.makeURLs(2);
			ChordWrapper peer2 = new ChordWrapper(urls[0], urls[1],
					bootsrap[0], bootsrap[1], "peer2/");

			System.out.println("created 3 peers");

			String s = "File version 1";
			peer0.fileManager.writeToRelativeFile("coolFile.txt", s.getBytes());

			System.out.println("Peer 0 uploading coolfile.txt");
			String[] torrentInfo = peer0.uploadFile("coolFile.txt");

			// assume peer0 has shared file with peer1 and peer2
			// as a consequence peer1 and peer2 add the file to their
			// fileManager
			peer1.fileManager.addFile("coolFile.txt", torrentInfo);
			peer2.fileManager.addFile("coolFile.txt", torrentInfo);

			peer1.sync("coolFile.txt");
			peer2.sync("coolFile.txt");

			peer1.fileManager.writeToRelativeFile("coolFile.txt",
					"File version 2".getBytes());
			System.out.println("Peer 1 edited file");

			peer1.update("coolFile.txt", torrentInfo);

			peer2.sync("coolFile.txt");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// expected output: p0 -> v4, p1 -> v2, p2 -> v3, p3 -> v4, p4 -> v5
	public static void test2() {
		try {

			PropertiesLoader.loadPropertyFile();
			URL[] bootsrap = TestUtils.makeURLs(0);

			System.out.println(Arrays.toString(bootsrap));
			ChordWrapper peer0 = new ChordWrapper(bootsrap[0], bootsrap[1],
					"owner/");

			URL[] urls = TestUtils.makeURLs(1);
			ChordWrapper peer1 = new ChordWrapper(urls[0], urls[1],
					bootsrap[0], bootsrap[1], "peer1/");

			urls = TestUtils.makeURLs(2);
			ChordWrapper peer2 = new ChordWrapper(urls[0], urls[1],
					bootsrap[0], bootsrap[1], "peer2/");

			urls = TestUtils.makeURLs(3);
			ChordWrapper peer3 = new ChordWrapper(urls[0], urls[1],
					bootsrap[0], bootsrap[1], "peer3/");

			urls = TestUtils.makeURLs(4);
			ChordWrapper peer4 = new ChordWrapper(urls[0], urls[1],
					bootsrap[0], bootsrap[1], "peer4/");

			System.out.println("created 5 peers");

			String filename = "coolFile.txt";
			String s1 = "File version 1";
			String s2 = "File version 2";
			String s3 = "File version 3";
			String s4 = "File version 4";
			String s5 = "File version 5";

			peer0.fileManager
					.writeToRelativeFile(filename, s1.getBytes()); // p0 -> v1

			System.out.println("Peer 0 uploading " + filename);
			String[] torrentInfo = peer0.uploadFile(filename);

			// assume peer0 has shared file with peers 1, 2 and 3
			// as a consequence peers 1, 2 and 3 add the file to their
			// fileManager
			System.out.println("Peer 0 shares file with peers 1, 2 and 3");
			peer1.fileManager.addFile(filename, torrentInfo); // p1 -> v1
			peer2.fileManager.addFile(filename, torrentInfo); // p2 -> v1
			peer3.fileManager.addFile(filename, torrentInfo); // p3 -> v1

			peer1.sync("coolFile.txt");

			System.out.println("Peer 1 edits and updates file");
			peer1.fileManager.writeToRelativeFile(filename, s2.getBytes());
			peer1.update(filename, torrentInfo); // p1 -> v2

			System.out.println("Peer 2 syncs file after 1 remote update");
			peer2.sync(filename); // p2 -> v2
			
			System.out.println("Peer 2 edits and updates file");
			peer2.fileManager.writeToRelativeFile(filename, s3.getBytes());
			peer2.update(filename, torrentInfo); // p2 -> v3
			
			System.out.println("Peer 3 syncs file after 2 remote updates");
			peer3.sync(filename); // p3 -> v3
			
			System.out.println("Peer 3 edits and updates file");
			peer3.fileManager.writeToRelativeFile(filename, s4.getBytes());
			peer3.update(filename, torrentInfo); // p3 -> v4
			
			System.out.println("Peer 0 syncs file after 3 remote update");
			peer0.sync(filename);
			
			System.out.println("Peer 0 shares file with Peer 4");
			peer4.fileManager.addFile(filename, torrentInfo); // p0 -> v4
			
			System.out.println("Peer 4 syncs after 1 remote update");
			peer4.sync(filename); // p4 -> v4
			
			System.out.println("Peer4 edits and updates file");
			peer2.fileManager.writeToRelativeFile(filename, s5.getBytes());
			peer2.update(filename, torrentInfo); // p4 -> v5

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		test2();
	}
}
