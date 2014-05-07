package tomPeer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.UUID;

import utils.Constants;
import net.tomp2p.connection.Bindings;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

public class SamplePeer {
	private Peer peer;
	private String peerID;
	private String shareFolderPath;

	// Create an identity
	// Listen for incoming connections
	// Bootstrap to a known peer in the P2P network
	// Start application specific communication

	public SamplePeer(InetAddress IPAddress, String bootstrapIP,
			String shareFolderPath) throws IOException {
		this.setShareFolderPath(shareFolderPath);

		// port and peerID need to set somehow
		// edit: createHash takes SHA1, so UUID can be used
		// random UUID must be stored on hdd after creation
		// new bindings, beyefham el interface lewa7do, (dont supply eth0 or
		// eth1)
		peer = new PeerMaker(Number160.createHash(UUID.randomUUID().toString()))
				.setPorts(Constants.PORT).setBindings(new Bindings(IPAddress))
				.makeAndListen();

		if (bootstrapIP != null
				&& !bootstrapIP.replaceAll("\\s+", "").isEmpty()) {
			// bootstrap to well-known IP
			if (!this.bootstrapper(bootstrapIP)) {
				System.out.println("failed!");
				// peer.shutdown(); // proper peer shutdown
			}
		}

		// add myself to DHT
		this.putPeerAddress(peer.getPeerAddress());

		// open peer for connections, somehow

	}

	private boolean bootstrapper(String bootstrapIP) {
		InetAddress bootstrapAddress = null;
		try {
			// get the InetAddress representation of the bootstrapIP (as in
			// tutorial)
			bootstrapAddress = InetAddress.getByName(bootstrapIP);
		} catch (Exception e) {
			return false;
		}

		FutureBootstrap future = peer.bootstrap()
				.setInetAddress(bootstrapAddress).setPorts(Constants.PORT)
				.start();
		future.awaitUninterruptibly();

		if (future.isFailed()) {

			System.out.println("failure");
			return false;
		}

		return true;
	}

	public void checkForUpdates() {

	}

	// might be boolean to make sure its there
	public void downloadFile() {

	}

	public void reportNewFile(String f) {

	}

	/**
	 * add an address to the DHT, so other peers can communicate with me ( 7asab
	 * mana fahem) peeraddress A PeerAddress contains the node ID and how to
	 * contact this node using both udp and tcp. edit: had to implement the
	 * getpeeraddress to get the tweak. i'll need to put the data in a
	 * serializable form
	 * 
	 * @param peerAddress
	 * @author saftophobia
	 */
	public void putPeerAddress(PeerAddress peerAddress) {
		FutureDHT fdht;
		ByteArrayOutputStream byteArrayOutputStream = null;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					byteArrayOutputStream);

			objectOutputStream.writeObject(peerAddress);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] byteArrayOutput = byteArrayOutputStream.toByteArray();

		// okay we need to "put" into the dht using the tutorial command
		// edit: the data used to be TEST, now i m gonna PUT the new
		// serializable
		try {
			fdht = peer.put(peerAddress.getID())
					.setData(new Data(byteArrayOutput)).start();

			// dont wait forever like the tutorial, set a time
			fdht.wait(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * retrieve address/object from the DHT, 7asab mana fahem bardo
	 * 
	 * @param hashInput
	 * @return
	 * @author saftophobia
	 */
	public PeerAddress getPeerAddress(String hashInput) {
		FutureDHT futureDHT = peer.get(new Number160(hashInput)).start();
		futureDHT.awaitUninterruptibly();
		if (futureDHT.isSuccess()) {
			Data data = futureDHT.getData();

			try {
				// Data is Serializable
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
						data.getData());

				ObjectInputStream objectInputStream = new ObjectInputStream(
						byteArrayInputStream);

				PeerAddress peerAddress = (PeerAddress) objectInputStream
						.readObject();

				// I should authenticate the peerAddress b4 returning, but wtv
				return peerAddress;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;

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
