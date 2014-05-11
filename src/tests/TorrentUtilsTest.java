package tests;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONArray;

import PeerBox.FileManager;
import PeerBox.TorrentUtils;

public class TorrentUtilsTest {

	/**
	 * @param args
	 */
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
		
		JSONArray jsonArr = TorrentUtils.convertTorrentContent(info);
		return (jsonArr.toJSONString().equals(getTestConfigString()));
	}
	
	public static boolean testWriteTorrentConfig() throws IOException{
		FileManager fm = new FileManager("tests" + File.separator + "peer_test");
		TorrentUtils.writeTorrentConfigToFile(fm, "test_torrent.json", getTestConfig());
		byte[] bytes = FileManager.readFile(fm.buildFullPath("test_torrent.json"));
		String s = new String(bytes);
		return s.equals(getTestConfigString());
	}
	public static void main(String[] args) throws IOException{
		System.out.println(TestBasicConversion());
		System.out.println(testWriteTorrentConfig());
	}
}
