package PeerBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class FileManager {
	public String workingDir;
	public HashMap<String, String[]> torrentInfos;
	public final int PIECE_SIZE = 512 * 1024;

	public FileManager(String folderName) {
		String projDir = System.getProperty("user.dir");

		this.workingDir = projDir + File.separator + "peersData"
				+ File.separator + folderName;

		torrentInfos = new HashMap<String, String[]>();

		// System.out.println("working dir : " + workingDir);
		File dir = new File(workingDir);
		if (!dir.exists())
			dir.mkdir();
	}

	public String[] getTorrentInfo(String filename) {
		return torrentInfos.get(filename);
	}
	
	public void addFile(String filename, String[] torrentInfo) {
		torrentInfos.put(filename, torrentInfo);
	}

	public void replaceTorrentInfo(String filename, String[] newTorrentInfo) {
		torrentInfos.remove(filename);
		torrentInfos.put(filename, newTorrentInfo);
	}

	public String buildFullPath(String relativePath) {
		return workingDir + File.separator + relativePath;
	}

	public byte[][] splitFiles(String relativePathToFile) throws IOException {
		File f = new File(buildFullPath(relativePathToFile));
		byte[][] splitted = FileSplitAndMerge.splitFile(f);
		return splitted;
	}

	public boolean writeToRelativeFile(String relativePath, byte[] content) {
		String fullPath = buildFullPath(relativePath);
		return FileManager.writeToAbsoluteFile(fullPath, content);
	}

	public static boolean writeToAbsoluteFile(String fullPath, byte[] content) {
		try {
			File file = new File(fullPath);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(content);
			fos.flush();
			fos.close();

		} catch (Exception e) {
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

}
