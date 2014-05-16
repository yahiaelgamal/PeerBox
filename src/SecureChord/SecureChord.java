package SecureChord;

import java.io.Serializable;
import java.util.Set;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.AsynChord;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.ChordCallback;
import de.uniba.wiai.lspi.chord.service.ChordFuture;
import de.uniba.wiai.lspi.chord.service.ChordRetrievalFuture;
import de.uniba.wiai.lspi.chord.service.Key;
import de.uniba.wiai.lspi.chord.service.Report;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class SecureChord implements Chord, Report, AsynChord{
	ChordImpl originalChord;

	public SecureChord() {
		originalChord = new ChordImpl();
	}
	
	public URL getURL() {
      return originalChord.getURL();
	}

	@Override
	public void setURL(URL nodeURL) throws IllegalStateException {
        originalChord.setURL(nodeURL);
	}

	@Override
	public ID getID() {
        return originalChord.getID();
	}

	@Override
	public void setID(ID nodeID) throws IllegalStateException {
        originalChord.setID(nodeID);
	}

	@Override
	public void create() throws ServiceException {
        originalChord.create();
	}

	@Override
	public void create(URL localURL) throws ServiceException {
        originalChord.create(localURL);
	}

	@Override
	public void create(URL localURL, ID localID) throws ServiceException {
        originalChord.create(localURL, localID);
	}

	@Override
	public void join(URL bootstrapURL) throws ServiceException {
        originalChord.join(bootstrapURL);
	}

	@Override
	public void join(URL localURL, URL bootstrapURL) throws ServiceException {
        originalChord.join(localURL, bootstrapURL);
	}

	@Override
	public void join(URL localURL, ID localID, URL bootstrapURL)
			throws ServiceException {
        originalChord.join(localURL, localID, bootstrapURL);
	}

	@Override
	public void leave() throws ServiceException {
        originalChord.leave();
	}

	@Override
	public void insert(Key key, Serializable object) throws ServiceException {
        originalChord.insert(key, object);
	}

	@Override
	public Set<Serializable> retrieve(Key key) throws ServiceException {
        return originalChord.retrieve(key);
	}

	@Override
	public void remove(Key key, Serializable object) throws ServiceException {
        originalChord.remove(key, object);
	}

	@Override
	public void retrieve(Key key, ChordCallback callback) {
        originalChord.retrieve(key, callback);
	}

	@Override
	public void insert(Key key, Serializable entry, ChordCallback callback) {
	    originalChord.insert(key, entry, callback);
	}

	@Override
	public void remove(Key key, Serializable entry, ChordCallback callback) {
        originalChord.remove(key, entry, callback);
		
	}

	@Override
	public ChordRetrievalFuture retrieveAsync(Key key) {
        return originalChord.retrieveAsync(key);
	}

	@Override
	public ChordFuture insertAsync(Key key, Serializable entry) {
        return originalChord.insertAsync(key, entry);
	}

	@Override
	public ChordFuture removeAsync(Key key, Serializable entry) {
       return originalChord.removeAsync(key, entry);
	}

	@Override
	public String printEntries() {
        return originalChord.printEntries();
	}

	@Override
	public String printFingerTable() {
        return originalChord.printFingerTable();
	}

	@Override
	public String printSuccessorList() {
        return originalChord.printSuccessorList();
	}

	@Override
	public String printReferences() {
        return originalChord.printReferences();
	}

	@Override
	public String printPredecessor() {
        return originalChord.printPredecessor();
	}
}
