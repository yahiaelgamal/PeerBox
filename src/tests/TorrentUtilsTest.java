package tests;

import java.awt.image.ConvolveOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import de.uniba.wiai.lspi.util.console.parser.ParseException;
import PeerBox.FileManager;
import PeerBox.TorrentConfig;
import PeerBox.Utils;

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

    public static void main(String[] args) throws Exception{
        System.out.println(TestBasicConversion());
        System.out.println(testWriteTorrentConfig());
        System.out.println(testTorrentConfig());
    }
}
