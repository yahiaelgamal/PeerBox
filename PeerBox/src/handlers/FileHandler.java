package handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * handles HDD accesses.
 * 
 * @author saftophobia
 * 
 */
public class FileHandler {

	public static void createFile(String path, byte[] data)
			throws IOException {
		File file = new File(path);
		if (file.isDirectory()) {
			System.out.println("Location is Dir");
			return;
		} else {
			File parentFolder = new File(file.getParent());
			if (!parentFolder.exists())
				parentFolder.mkdirs();
		}

		if (file.exists())
			throw new IOException("File already exists");

		file.createNewFile();

		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write(data);
		fileOutputStream.close();
	}
}
