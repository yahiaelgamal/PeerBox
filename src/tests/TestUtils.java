package tests;

import java.net.MalformedURLException;

import de.uniba.wiai.lspi.chord.data.URL;

import PeerBox.ChordWrapper;

public class TestUtils {

	public static URL[] makeURLs(int i) throws MalformedURLException {
		URL localURL0 = new URL(ChordWrapper.PROTOCOL + "://localhost:"
				+ (4000 + i) + "/");
		URL localURL1 = new URL(ChordWrapper.PROTOCOL + "://localhost:"
				+ (5000 + i) + "/");
		URL localURL2 = new URL(ChordWrapper.PROTOCOL + "://localhost:"
				+ (6000 + i) + "/");
		return new URL[] { localURL0, localURL1, localURL2 };

	}
}
