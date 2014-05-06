package tomPeer;

import net.tomp2p.p2p.Peer;

public class SamplePeer {
	private Peer peer;
	private String peerID;
	private String shareFolderPath;

	public SamplePeer() {

	}

	public void checkForUpdates() {

	}
	
	//might be boolean to make sure its there
	public void downloadFile(){
		
	}
	
	public void reportNewFile(String f)
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
