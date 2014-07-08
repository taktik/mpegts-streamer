package org.taktik.mpegts;


public class MTSSources {
	public static MTSSource fromSources(MTSSource... sources) {
		return new MultiMTSSource(sources);
	}
}
