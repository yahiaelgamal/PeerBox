import java.io.IOException;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class SamplePeer {

	// Create an identity
	// Listen for incoming connections
	// Bootstrap to a known peer in the P2P network
	// Start application specific communication

	final private Peer peer;

	// initializer
	public SamplePeer(int peerID) throws IOException {
		// initialize the peer with PeerMaker, it generates a Random number
		// between 0 and peerID, and initialize a Number160 value for the
		// peerMaker.
		// Then, sets the port, and set the config for makeandlisten;
		this.peer = new PeerMaker(Number160.createHash(peerID)).setPorts(
				4000 + peerID).makeAndListen();

		// Used for bootstrapping. One important information in bootstrapping is
		// to get the nodes that we bootstrapped to. We may not know this in
		// advance as we might bootstrap via broadcast.
		FutureBootstrap fb = peer.bootstrap().setBroadcast().setPorts(4001)
				.start();

		// Wait for the asynchronous operation to end without interruption.
		fb.awaitUninterruptibly();

		// Returns the Peers we bootstrapped in no particular order.
		if (fb.getBootstrapTo() != null) {

			// msh fahem, does he set the peer address to the first
			// hooked/bootstrapped peer he finds?
			peer.discover()
					.setPeerAddress(fb.getBootstrapTo().iterator().next())
					.start().awaitUninterruptibly();

		}
	}

	private String get(String name) throws ClassNotFoundException, IOException {
		FutureDHT futureDHT = peer.get(Number160.createHash(name)).start();
		futureDHT.awaitUninterruptibly();
		if (futureDHT.isSuccess()) {
			return futureDHT.getData().getObject().toString();
		}
		return "not found";
	}

	private void store(String name, String ip) throws IOException {
		peer.put(Number160.createHash(name)).setData(new Data(ip)).start()
				.awaitUninterruptibly();
	}

	public static void main(String[] args) throws NumberFormatException,
			IOException, ClassNotFoundException {
		SamplePeer samplePeer = new SamplePeer(Integer.parseInt(args[0]));
		if (args.length == 3) {
			// store arguments
			samplePeer.store(args[1], args[2]);
		}
		if (args.length == 2) {
			System.out.println("Name:" + args[1] + " IP:"
					+ samplePeer.get(args[1]));
		}
	}
}
