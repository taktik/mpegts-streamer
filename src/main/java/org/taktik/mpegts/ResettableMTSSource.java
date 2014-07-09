package org.taktik.mpegts;

public interface ResettableMTSSource extends MTSSource {
	public void reset() throws Exception;
}
