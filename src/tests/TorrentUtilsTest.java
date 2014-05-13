package tests;

import java.io.File;
import java.io.IOException;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.util.console.parser.ParseException;
import PeerBox.ChordWrapper;
import PeerBox.FileManager;
import PeerBox.TorrentConfig;

public class TorrentUtilsTest {

//    public static ArrayList<ArrayList<String>> getTestConfig(){
    public static String[][] getTestConfig(){
        String[][] info = { {"key1", "EncKey1", "init1"},
                            {"key2", "EncKey2", "init2"},
                            {"key3", "EncKey3", "init3"} };
//        ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
//        for(String[] piece : info) {
//        	ArrayList<String> pieceAsAList= Utils.convertFromArrayToArrayList(piece);
//        	list.add(pieceAsAList);
//        }
        return info;
    }
    

    public static String getTestConfigString() {
        return "{\"pieces\":[[\"key1\",\"EncKey1\",\"init1\"],[\"key2\",\"EncKey2\",\"init2\"],[\"key3\",\"EncKey3\",\"init3\"]],\"filename\":\"filename\"}";
    }
    public static boolean TestBasicConversion() {
//        ArrayList<ArrayList<String>> info = getTestConfig();
        String[][] info = getTestConfig();
        String filename = "filename";

        TorrentConfig tc = new TorrentConfig(filename, info);
        return (tc.toJSONString().equals(getTestConfigString()));
    }

    public static boolean testWriteTorrentConfig() throws IOException{
        FileManager fm = new FileManager("tests" + File.separator + "peer_test");
        TorrentConfig tc = new TorrentConfig("filename", getTestConfig());
        tc.writeToFile(fm.buildFullPath("test_torrent.json"));
        byte[] bytes = FileManager.readFile(fm.buildFullPath("test_torrent.json"));
        String s = new String(bytes);
        return s.equals(getTestConfigString());
    }

    public static boolean testTorrentConfig() throws ParseException, IOException, org.json.simple.parser.ParseException {
        FileManager fm = new FileManager("tests" + File.separator + "peer_test");
        TorrentConfig tc = new TorrentConfig("filename", getTestConfig());

        // rebuild it 
        tc = new TorrentConfig(fm.buildFullPath("test_torrent.json"));
        return tc.getAllKeys().get(0).equals("key1") &&
               tc.getAllKeys().get(1).equals("key2") &&
               tc.getAllKeys().get(2).equals("key3");
    }

    public static void testLocalNetwork(String PROTOCOL)
    {
    	 System.out.println(System.getProperty("java.class.path"));
		 int nrPeers = 10;
		 try {
		 PropertiesLoader.loadPropertyFile();
		
		 URL localURL1 = new URL(PROTOCOL + "://localhost:4000/");
		 URL localURL2 = new URL(PROTOCOL + "://localhost:6000/");
		 URL localURL3 = new URL(PROTOCOL + "://localhost:8000/");
		 ChordWrapper first = new ChordWrapper(localURL1, localURL2, localURL3,
		 "peer0/");
		 System.out.println("Created first peer");
		
		 ChordWrapper[] wrappers = new ChordWrapper[nrPeers];
		 wrappers[0] = first;
		
		 for (int i = 1; i < nrPeers; i++) {
			 int port1 = 4000 + i;
			 int port2 = 6000 + i;
			 int port3 = 8000 + i;
			 URL newURL1 = new URL(PROTOCOL + "://localhost:" + port1 + "/");
			 URL newURL2 = new URL(PROTOCOL + "://localhost:" + port2 + "/");
			 URL newURL3 = new URL(PROTOCOL + "://localhost:" + port3 + "/");
			
			 // localURL (URL for someone in the network) will be known by a
			 // higher level discovery mechanism
			 wrappers[i] = new ChordWrapper(newURL1, newURL2, newURL3, localURL1,
			 localURL2, localURL3, "peer" + i + "/");
		 }
		
		 System.out.println("peer[0] is splitting files");
		 String[] torrentInfo = wrappers[0].uploadFile("IMG_8840.JPG");
		 System.out.println("peer[0] split ended");
		
		 // assumption of knowing the keys
		 // JUST FOR TESTING peer2 will retreive the picture
		 System.out.println("Peer 2 is getting peices .. ");
		 wrappers[2].fileManager.addFile("retreivedFile.jpg", torrentInfo);
		 wrappers[2].sync("retreivedFile.jpg");
		 System.out.println("check peer2 folder for a surprise");
		 // VOALA WE HAVE A DROPBOX
		
		 // go to console and try retrieving the hashes, it will work !
		
		 } catch (Exception e) {
		 e.printStackTrace();
		 System.exit(1);
		 }
    }
    
    public static void main(String[] args) throws Exception{
        System.out.println(TestBasicConversion());
        System.out.println(testWriteTorrentConfig());
        System.out.println(testTorrentConfig());
        testLocalNetwork(ChordWrapper.PROTOCOL);
    }
}
