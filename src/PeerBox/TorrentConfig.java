package PeerBox;

import java.io.IOException;
import java.util.ArrayList;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class TorrentConfig {


	// key, encryption key, init vector
	ArrayList<ArrayList<String>> config;
	
	public TorrentConfig(String jsonFileFullPath) throws IOException, ParseException {
		byte[] bytes = FileManager.readFile(jsonFileFullPath);
		String str = new String(bytes);
		Object obj = JSONValue.parse(str);
		config = (ArrayList<ArrayList<String>>) obj;
	}
	
	public ArrayList<String> getAllKeys(){
		ArrayList<String> allKeys = new ArrayList<String>(config.size());
		for(ArrayList<String> piece : config) {
			allKeys.add(piece.get(0));
		}
		return allKeys;
	}
	
	public static void main(String[] args) throws Exception{
		String fullPath = "/Users/yahiaelgamal/Documents/workspace/openChord/peersData/tests/peer_test/test_torrent.json";
		TorrentConfig tc = new TorrentConfig(fullPath);
		System.out.println(tc.getAllKeys());
	}
}
