package PeerBox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class FileManager {
	public String workingDir;
	public HashMap<String, String[]> torrentInfos;
	public HashMap<String, TorrentConfig> torrentConfigs;
	public final int PIECE_SIZE = 512 * 1024;

	public FileManager(String folderName) {
		String projDir = System.getProperty("user.dir");

		this.workingDir = projDir + File.separator + "peersData"
				+ File.separator + folderName;

		torrentInfos = new HashMap<String, String[]>();
		torrentConfigs = new HashMap<String, TorrentConfig>();

		// System.out.println("working dir : " + workingDir);
		File dir = new File(workingDir);
		if (!dir.exists())
			dir.mkdir();
	}

	public String[] getTorrentInfo(String filename) {
		return torrentInfos.get(filename);
	}
	
	public TorrentConfig getTorrentConfig(String filename) {
		return torrentConfigs.get(filename);
	}
	
	public void addFile(String filename, String[] torrentInfo) {
		if (torrentInfos.containsKey(filename)) {
			torrentInfos.put(filename, torrentInfo);
		}
		else {
			torrentInfos.remove(filename);
			torrentInfos.put(filename, torrentInfo);
		}
	}


	public String buildFullPath(String relativePath) {
		return workingDir + File.separator + relativePath;
	}

	public byte[][] splitFiles(String relativePathToFile) throws IOException {
		File f = new File(buildFullPath(relativePathToFile));
		byte[][] splitted = FileSplitAndMerge.splitFile(f);
		return splitted;
	}

	public boolean writeToRelativeFile(String relativePath, byte[] content, boolean append) {
		String fullPath = buildFullPath(relativePath);
		return FileManager.writeToAbsoluteFile(fullPath, content, append);
	}

	public static boolean writeToAbsoluteFile(String fullPath, byte[] content, boolean append) {
		try {
			File file = new File(fullPath);
			FileOutputStream fos = new FileOutputStream(file, append);
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
	
	public void savePrivateKey(PrivateKey privateKey) throws IOException{  
		String fileName = "Private.key";
		FileOutputStream fos = null;  
		ObjectOutputStream oos = null;  

		try {  

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
			RSAPrivateKeySpec rsaPrivKeySpec = keyFactory.getKeySpec(
					privateKey, RSAPrivateKeySpec.class);
			
			fos = new FileOutputStream(fileName);  
			oos = new ObjectOutputStream(new BufferedOutputStream(fos));  

			oos.writeObject(rsaPrivKeySpec.getModulus());  
			oos.writeObject(rsaPrivKeySpec.getPrivateExponent());     

			System.out.println(fileName + " generated successfully");  
		} catch (Exception e) {  
			e.printStackTrace();  
		}  
		finally{  
			if(oos != null){  
				oos.close();  

				if(fos != null){  
					fos.close();  
				}  
			}  
		}    
	}
	public PrivateKey readPrivateKey(){
		String fileName = "Private.key";
		FileInputStream fis = null;  
		ObjectInputStream ois = null;  
		try {  
			fis = new FileInputStream(new File(fileName));  
			ois = new ObjectInputStream(fis);  

			BigInteger modulus = (BigInteger) ois.readObject();  
			BigInteger exponent = (BigInteger) ois.readObject();  

			//Get Private Key  
			RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);  
			KeyFactory fact = KeyFactory.getInstance("RSA");  
			PrivateKey privateKey = fact.generatePrivate(rsaPrivateKeySpec);  

			return privateKey;  

		} catch (Exception e) {  
			e.printStackTrace();  
		}  
		finally{  
			if(ois != null){  
				try {
					ois.close();
					if(fis != null){  
						fis.close();  
					}  
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
			}  
		}  
		return null;  
	} 

}
