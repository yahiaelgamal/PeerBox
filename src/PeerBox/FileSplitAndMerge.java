package PeerBox;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

// Splits files, format independent and merges them back
public class FileSplitAndMerge {

    public static void mergeFiles(String filename, LinkedList<String> paths) throws IOException{
        File out = new File(filename);
        FileOutputStream fos;
        byte[] fileBytes;
        fos = new FileOutputStream(out,true);
        for (String path: paths) {
            fileBytes = FileManager.readFile(path);
            fos.write(fileBytes);
            fos.flush();
            fileBytes = null;
        }
        fos.close();
        fos = null;
    }
    
    public static byte[][] splitFile(File f) throws IOException {
    	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
    	
    	int sizeOfPiece = 512 * 1024;
    	int numPieces = (int) Math.ceil(f.length() * 1.0 / sizeOfPiece);
    	
    	byte[][] pieces = new byte[numPieces][sizeOfPiece];
    	
        int i = 0;
        byte[] buffer = new byte[sizeOfPiece];
        while (bis.read(buffer) > 0) {
        	pieces[i++] = buffer.clone();
        }
        
        bis.close();
        return pieces;
    }

}
