package PeerBox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Set;

import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class ChordWrapper {

    // use over real network
    static String PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

    // use for testing on the JVM/thread
//  String static PROTOCOL = URL.KNOWN_PROTOCOLS.get(URL.LOCAL_PROTOCOL);

    public Chord chord;
    public FileManager fileManager;

    // In case of a creator
    public ChordWrapper(URL myURL, String myFolder) {
        try {
            this.chord = new ChordImpl();
            this.chord.create(myURL);
            this.fileManager = new FileManager(myFolder);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // In case of bootstraper
    public ChordWrapper(URL myURL, URL bootstrapURL, String myFolder) {
        try {
            this.chord = new ChordImpl();
            this.chord.join(myURL, bootstrapURL);
            this.fileManager = new FileManager(myFolder);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    // assumes pieces of peroper size
    public Key[] insertPieces(LinkedList<String> pieces) throws IOException, ServiceException {

        // TODO load the pieces (why save it in the first place)
        Key[] keys = new Key[pieces.size()];
        int i =0;
        for(String filepath : pieces) {
            File f = new File(filepath);
            System.out.println("Reading " + filepath);
            BufferedInputStream bis = new BufferedInputStream( new FileInputStream(f));
            byte[] buffer = new byte[fileManager.PIECE_SIZE];
            bis.read(buffer);
            bis.close();

            // TODO insert piece
            System.out.println("sending bytes ..");
            Key key = insertPiece(buffer);
            System.out.println("Sent with hash " + key);
            keys[i] = key;
            i++;
        }
        return keys;
    }

    public Key insertPiece(Serializable data) throws ServiceException  {
        // TODO hash the piece
        Key key = new Key(data.hashCode()+"");

        // TODO encrypt the piece
        this.chord.insert(key, data);
        return key;
    }

    public Set<Serializable> getPiece(Key key) throws ServiceException {
        return this.chord.retrieve(key);
    }

    public static void main(String[] args){

        System.out.println(System.getProperty("java.class.path"));
        int nrPeers = 10;
        try {
            PropertiesLoader.loadPropertyFile();

            URL localURL = new URL(PROTOCOL + "://localhost:8000/");
            ChordWrapper first = new ChordWrapper(localURL, "peer0/");
            System.out.println("Created first peer");


            ChordWrapper[] wrappers = new ChordWrapper[nrPeers];
            wrappers[0] = first;

            for(int i = 1; i < nrPeers; i++) {
                int port = 8000 + i;

                URL newURL = new URL(PROTOCOL + "://localhost:" + port + "/");

                // localURL (URL for someone in the network) will be known by a
                // higher level discovery mechanism
                wrappers[i] = new ChordWrapper(newURL, localURL, "peer"+i+"/");
            }

            System.out.println("peer[0] is splitting files");
            LinkedList<String> pieces = wrappers[0].fileManager.splitFiles("IMG_8840.JPG");
            System.out.println("peer[0] split ended");

            Key[] keys = wrappers[0].insertPieces(pieces);

            // assumption of knowing the keys
            // JUST FOR TESTING peer2 will retreive the picture
            FileOutputStream fos = new FileOutputStream(
                    wrappers[2].fileManager.buildFullPath("retrievedFile.jpg"),true);


            System.out.println("Peer 2 is getting peices .. ");
            byte[] fileBytes;
            for(int i = 0; i< keys.length; i++) {
                System.out.println("Getting piece " + i);
                Set<Serializable> set = wrappers[2].getPiece(keys[i]);
                fileBytes = (byte[])set.toArray()[0];
                fos.write(fileBytes);
                fos.flush();
            }
            fos.close();
            System.out.println("check peer2 folder for a surprise");
            // VOALA WE HAVE A DROPBOX

            // go to console and try retrieving the hashes, it will work !

        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
