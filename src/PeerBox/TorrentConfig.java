package PeerBox;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class TorrentConfig {

	// contains two elements (filename, and pieces) where each piece is
	// an ArrayList [key, encryption_key, init_vector]
	public JSONObject map;

	public TorrentConfig(String jsonFileFullPath) throws IOException,
			ParseException {
		byte[] bytes = FileManager.readFile(jsonFileFullPath);
		String str = new String(bytes);
		Object obj = JSONValue.parse(str);
		this.map = (JSONObject) obj;
	}

	public TorrentConfig(byte[] bytes) throws IOException, ParseException {
		String str = new String(bytes);
		Object obj = JSONValue.parse(str);
		this.map = (JSONObject) obj;
	}

	// // REMOVE DUPLICATE
	public TorrentConfig(String filename, String[][] piecesInfo) {
		// extends ArrayList
		JSONArray jsonArr = new JSONArray();
		for (String[] pieceInfo : piecesInfo) {
			JSONArray jsonArr2 = new JSONArray();
			for (String value : pieceInfo) {
				jsonArr2.add(value);
			}
			jsonArr.add(jsonArr2);
		}
		this.map = new JSONObject();
		map.put("filename", filename);
		map.put("pieces", jsonArr);
	}

	// public ArrayList<ArrayList<String>> getAllPiecesInfo() {
	public ArrayList<ArrayList<String>> getAllPiecesInfo() {
		int numPieces = ((ArrayList) map.get("pieces")).size();
		String[][] allPiecesArray = new String[numPieces][3];
		return (ArrayList) map.get("pieces");
	}

	public ArrayList<String> getAllKeys() {
		ArrayList<ArrayList<String>> pieces = (JSONArray) map.get("pieces");
		ArrayList<String> allKeys = new ArrayList<String>(pieces.size());
		for (ArrayList<String> piece : pieces) {
			allKeys.add(piece.get(0));
		}
		return allKeys;
	}

	public String toJSONString() {
		String jsonArrString = map.toJSONString();
		return jsonArrString;
	}

	public boolean writeToFile(String fullPath) {
		return FileManager.writeToAbsoluteFile(fullPath, this.toJSONString()
				.getBytes());
	}

	public Object get(String key) {
		return map.get(key);
	}
	
	public static void main(String[] args) {
		byte[] data1 = new byte[] {(byte)0, (byte)2};
		byte[] data2 = new byte[] {(byte)127, (byte)2};
		System.out.println(Utils.isTorrentFile(data1));
		System.out.println(Utils.isTorrentFile(data2));
	}

}
