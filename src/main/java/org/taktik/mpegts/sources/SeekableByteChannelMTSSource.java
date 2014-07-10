package org.taktik.mpegts.sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.google.common.base.Preconditions;
import org.taktik.ioutils.NIOUtils;
import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

public class SeekableByteChannelMTSSource implements ResettableMTSSource {

	private SeekableByteChannel byteChannel;

	private SeekableByteChannelMTSSource(SeekableByteChannel byteChannel) throws IOException {
		this.byteChannel = byteChannel;
	}

	@Override
	public MTSPacket nextPacket() throws IOException {
		// Get next packet
		ByteBuffer buffer = ByteBuffer.allocate(Constants.MPEGTS_PACKET_SIZE);
		if (NIOUtils.read(byteChannel, buffer) != Constants.MPEGTS_PACKET_SIZE) {
			return null;
		}
		buffer.flip();

		// Parse the packet
		return new MTSPacket(buffer);
	}

	public static SeekableByteChannelMTSSourceBuilder builder() {
		return new SeekableByteChannelMTSSourceBuilder();
	}

	@Override
	public void reset() throws IOException {
		byteChannel.position(0);
	}

	public static class SeekableByteChannelMTSSourceBuilder {
		private SeekableByteChannel byteChannel;

		public SeekableByteChannelMTSSourceBuilder setByteChannel(SeekableByteChannel byteChannel) {
			this.byteChannel = byteChannel;
			return this;
		}

		public SeekableByteChannelMTSSource build() throws IOException {
			Preconditions.checkNotNull(byteChannel, "byteChannel cannot be null");
			return new SeekableByteChannelMTSSource(byteChannel);
		}
	}
}
