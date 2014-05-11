package tests;

import org.json.simple.JSONArray;

import PeerBox.TorrentUtils;

public class TorrentUtilsTest {

	/**
	 * @param args
	 */
	public static boolean TestBasicConversion() {
		String[][] info = { {"key1", "EncKey1", "init1"},
							{"key2", "EncKey2", "init2"},
							{"key3", "EncKey3", "init3"} };
		
		JSONArray jsonArr = TorrentUtils.convertTorrentContent(info);
		return (jsonArr.toJSONString().equals( 
				"[[\"key1\",\"EncKey1\",\"init1\"],[\"key2\",\"EncKey2\",\"init2\"],[\"key3\",\"EncKey3\",\"init3\"]]"));
	}
	public static void main(String[] args) {
		System.out.println(TestBasicConversion());
	}
}
