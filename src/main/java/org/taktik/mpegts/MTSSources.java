package org.taktik.mpegts;


public class MTSSources {
	public static MTSSource fromSources(MTSSource... sources) {
		return  MultiMTSSource.builder()
				.setFixContinuity(false)
				.setSources(sources)
				.build();
	}
}
