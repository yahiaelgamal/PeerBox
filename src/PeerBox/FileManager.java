package PeerBox;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

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
	
	public LinkedList <String> splitFiles(String relativePathToFile) throws IOException {
		File f = new File(buildFullPath(relativePathToFile));
		LinkedList <String> splitted = FileSplitAndMerge.splitFile(f);
		return splitted;
	}
	
}
