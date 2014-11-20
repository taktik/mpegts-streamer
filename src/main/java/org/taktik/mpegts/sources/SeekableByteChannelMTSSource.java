package org.taktik.mpegts.sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.ioutils.NIOUtils;
import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

public class SeekableByteChannelMTSSource extends AbstractByteChannelMTSSource<SeekableByteChannel> implements ResettableMTSSource {

	private SeekableByteChannelMTSSource(SeekableByteChannel byteChannel) throws IOException {
		super(byteChannel);
	}

	public static SeekableByteChannelMTSSourceBuilder builder() {
		return new SeekableByteChannelMTSSourceBuilder();
	}

	@Override
	public void reset() throws IOException {
		byteChannel.position(0);
		fillBuffer();
	}

	public static class SeekableByteChannelMTSSourceBuilder {
		private SeekableByteChannel byteChannel;

		private SeekableByteChannelMTSSourceBuilder(){}

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
