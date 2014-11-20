package org.taktik.mpegts.sources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import com.google.common.base.Preconditions;
import org.taktik.ioutils.NIOUtils;
import org.taktik.mpegts.Constants;
import org.taktik.mpegts.MTSPacket;

public class ByteChannelMTSSource extends AbstractByteChannelMTSSource<ByteChannel> {

	private ByteChannelMTSSource(ByteChannel byteChannel) throws IOException {
		super(byteChannel);
	}

	public static ByteChannelMTSSourceBuilder builder() {
		return new ByteChannelMTSSourceBuilder();
	}

	public static class ByteChannelMTSSourceBuilder {
		private ByteChannel byteChannel;

		private ByteChannelMTSSourceBuilder(){}

		public ByteChannelMTSSourceBuilder setByteChannel(ByteChannel byteChannel) {
			this.byteChannel = byteChannel;
			return this;
		}

		public ByteChannelMTSSource build() throws IOException {
			Preconditions.checkNotNull(byteChannel, "byteChannel cannot be null");
			return new ByteChannelMTSSource(byteChannel);
		}
	}
}
