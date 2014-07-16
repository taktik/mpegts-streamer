package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

public interface MTSSource {
	public MTSPacket nextPacket() throws Exception;
	public void close() throws Exception;
}
