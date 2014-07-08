package org.taktik.mpegts;

public interface MTSSource {
	public MTSPacket nextPacket() throws Exception;
}
