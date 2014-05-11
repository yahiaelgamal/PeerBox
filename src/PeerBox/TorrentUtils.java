package PeerBox;

import org.json.simple.JSONArray;

public class TorrentUtils {
	@SuppressWarnings("unchecked")
	public static JSONArray convertTorrentContent(String[][] piecesInfo) {
		// extends ArrayList
		JSONArray jsonArr = new JSONArray();
		for(String[] pieceInfo: piecesInfo) {
			JSONArray jsonArr2 = new JSONArray();
			for(String value : pieceInfo) {
				jsonArr2.add(value);
			}
			jsonArr.add(jsonArr2);
		}

		return jsonArr;
	}
	
	// TODO test the method
	public static boolean writeTorrentConfigToFile(FileManager fm,
												   String torrentFileName,
												   String[][] piecesInfo) {
		JSONArray jsonArr = convertTorrentContent(piecesInfo);
		String jsonArrString = jsonArr.toJSONString();
		return fm.writeToFile(torrentFileName, jsonArrString.getBytes());
	}
}
