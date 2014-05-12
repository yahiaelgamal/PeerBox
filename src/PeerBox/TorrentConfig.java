package PeerBox;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class TorrentConfig {

    // key, encryption key, init vector
    public ArrayList<ArrayList<String>> config;

    public TorrentConfig(String jsonFileFullPath) throws IOException, ParseException {
        byte[] bytes = FileManager.readFile(jsonFileFullPath);
        String str = new String(bytes);
        Object obj = JSONValue.parse(str);
        this.config = (JSONArray) obj;
    }
    
    public TorrentConfig(byte[] bytes) throws IOException, ParseException {
        String str = new String(bytes);
        Object obj = JSONValue.parse(str);
        this.config = (JSONArray) obj;
    }
    
    // REMOVE DUPLICATE
    public TorrentConfig(ArrayList<ArrayList<String>> piecesInfo) {
        JSONArray jsonArr = new JSONArray();
        for(ArrayList<String> pieceInfo: config) {
            JSONArray jsonArr2 = new JSONArray();
            for(String value : pieceInfo) {
                jsonArr2.add(value);
            }
            jsonArr.add(jsonArr2);
        }
        this.config = jsonArr;
    }
    
    // REMOVE DUPLICATE
    public TorrentConfig(String[][] piecesInfo) {
        // extends ArrayList
        JSONArray jsonArr = new JSONArray();
        for(String[] pieceInfo: piecesInfo) {
            JSONArray jsonArr2 = new JSONArray();
            for(String value : pieceInfo) {
                jsonArr2.add(value);
            }
            jsonArr.add(jsonArr2);
        }
        this.config = jsonArr;
    }
    
    public String[][] getAllPiecesInfo() {
    	 String[][] allPiecesInfo = new String[config.size()][3];
    	 
    	 String[] pieceInfo;
    	 int i = 0;
         for(ArrayList<String> piece : config) {
        	 pieceInfo = new String[3];
        	 
        	 pieceInfo[0] = piece.get(0);
        	 pieceInfo[1] = piece.get(1);
        	 pieceInfo[2] = piece.get(2);
        	 
        	 allPiecesInfo[i++] = pieceInfo;
         }
         return allPiecesInfo;
    }
    
    public ArrayList<String> getAllKeys(){
        ArrayList<String> allKeys = new ArrayList<String>(config.size());
        for(ArrayList<String> piece : config) {
            allKeys.add(piece.get(0));
        }
        return allKeys;
    }

    public String toJSONString() {
        String jsonArrString = ((JSONArray)config).toJSONString();	 
        return jsonArrString;
    }
    
    public boolean writeToFile(String fullPath) {
    	return FileManager.writeToAbsoluteFile(fullPath, this.toJSONString().getBytes());
    }
    
    public static void main(String[] args) throws Exception{
        String fullPath = "/Users/yahiaelgamal/Documents/workspace/openChord/peersData/tests/peer_test/test_torrent.json";
        TorrentConfig tc = new TorrentConfig(fullPath);
        System.out.println(tc.getAllKeys());
    }
}
