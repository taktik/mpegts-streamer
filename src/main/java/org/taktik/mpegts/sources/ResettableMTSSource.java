package org.taktik.mpegts.sources;

public interface ResettableMTSSource extends MTSSource {
	public void reset() throws Exception;
}
