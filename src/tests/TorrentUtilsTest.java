package tests;

import java.io.File;
import java.io.IOException;

import de.uniba.wiai.lspi.util.console.parser.ParseException;
import PeerBox.FileManager;
import PeerBox.TorrentConfig;

public class TorrentUtilsTest {

    public static String[][] getTestConfig(){
        String[][] info = { {"key1", "EncKey1", "init1"},
                            {"key2", "EncKey2", "init2"},
                            {"key3", "EncKey3", "init3"} };
        return info;
    }

    public static String getTestConfigString() {
        return "[[\"key1\",\"EncKey1\",\"init1\"],[\"key2\",\"EncKey2\",\"init2\"],[\"key3\",\"EncKey3\",\"init3\"]]";
    }
    public static boolean TestBasicConversion() {
        String[][] info = getTestConfig();

        TorrentConfig tc = new TorrentConfig(info);
        return (tc.toJSONString().equals(getTestConfigString()));
    }

    public static boolean testWriteTorrentConfig() throws IOException{
        FileManager fm = new FileManager("tests" + File.separator + "peer_test");
        TorrentConfig tc = new TorrentConfig(getTestConfig());
        tc.writeToFile(fm.buildFullPath("test_torrent.json"));
        byte[] bytes = FileManager.readFile(fm.buildFullPath("test_torrent.json"));
        String s = new String(bytes);
        return s.equals(getTestConfigString());
    }

    public static boolean testTorrentConfig() throws ParseException, IOException, org.json.simple.parser.ParseException {
        FileManager fm = new FileManager("tests" + File.separator + "peer_test");
        TorrentConfig tc = new TorrentConfig(getTestConfig());

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
