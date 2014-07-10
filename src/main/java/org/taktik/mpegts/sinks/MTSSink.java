package org.taktik.mpegts.sinks;

import org.taktik.mpegts.MTSPacket;

public interface MTSSink {
	public void send(MTSPacket packet) throws Exception;
}
