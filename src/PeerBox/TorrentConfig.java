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
        config = (JSONArray) obj;
    }
    
    public TorrentConfig(ArrayList config) {
    	this.config = config;
    }
    
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
