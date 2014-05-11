package PeerBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.uniba.wiai.lspi.chord.console.command.entry.Key;

public class FileManager {
    public String workingDir;
    public final  int PIECE_SIZE = 512 * 1024;

    public FileManager(String folderName) {
        String projDir = System.getProperty("user.dir");

        this.workingDir = projDir + File.separator + "peersData" + File.separator + folderName;

        System.out.println("working idr : " + workingDir);
        File dir = new File(workingDir);
        if(!dir.exists())
            dir.mkdir();
    }

    public String buildFullPath(String relativePath) {
        return workingDir + File.separator + relativePath;
    }

    public LinkedList<String> splitFiles(String relativePathToFile) throws IOException {
        File f = new File(buildFullPath(relativePathToFile));
        LinkedList <String> splitted = FileSplitAndMerge.splitFile(f);
        return splitted;
    }

    public boolean writeToFile(String relativePath, byte[] content) {
        try {
            String fullPath = buildFullPath(relativePath);
            File file = new File(fullPath);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content);
            fos.flush();
            fos.close();

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String convertMapToJSONString(Map map) {
        String jsonString = JSONObject.toJSONString(map);
        return jsonString;
    }

//  @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Key key = new Key("key 1");
    }

}
