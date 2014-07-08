package org.taktik.mpegts;

public interface MTSSink {
	public void send(MTSPacket packet) throws Exception;
}
