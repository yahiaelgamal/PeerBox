package tomPeer;

import java.io.IOException;
import java.net.InetAddress;

import net.tomp2p.connection.Bindings;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;

public class SamplePeer {
	private Peer peer;
	private String peerID;
	private String shareFolderPath;

	
	public SamplePeer(InetAddress IPAddress, String bootstrapIP, String shareFolderPath) throws IOException {
		this.setShareFolderPath(shareFolderPath);
		
		//port and peerID need to set somehow
		peer = new PeerMaker(Number160.createHash(this.getPeerID())).setPorts(123).setBindings(new Bindings(IPAddress)).makeAndListen();
		
		if(bootstrapIP != null && !bootstrapIP.replaceAll("\\s+","").isEmpty()){
			//bootstrap to well-known IP
		}
		//add myself to DHT
		//open peer for connections, somehow
	}

	public void checkForUpdates() {

	}
	
	//might be boolean to make sure its there
	public void downloadFile(){
		
	}
	
	public void reportNewFile(String f)
	{
		
	}
	
	//add an address to the DHT, so other peers can share ( 7asab mana fahem)
	public void putPeerAddress()
	{
		
	}
	
	//retrieve address/object from the DHT, 7asab mana fahem bardo
	public void getPeerAddress(/* hash */ )
	{
		
	}
	
	

	/**
	 * setters and getters ShareFolder's should be customized
	 * ***********************************************
	 * 
	 * @return
	 */
	public String getShareFolderPath() {
		return shareFolderPath;
	}

	public void setShareFolderPath(String shareFolderPath) {
		this.shareFolderPath = shareFolderPath;
	}

	public Peer getPeer() {
		return peer;
	}

	public void setPeer(Peer peer) {
		this.peer = peer;
	}

	public String getPeerID() {
		return peerID;
	}

	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}

}
