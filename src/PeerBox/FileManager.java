package PeerBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
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

//        System.out.println("working dir : " + workingDir);
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
    
    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
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
