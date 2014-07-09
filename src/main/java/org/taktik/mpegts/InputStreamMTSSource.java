package org.taktik.mpegts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;

public class InputStreamMTSSource implements MTSSource {

	private InputStream inputStream;

	private InputStreamMTSSource(InputStream inputStream) throws IOException {
		this.inputStream = inputStream;
	}

	@Override
	public MTSPacket nextPacket() throws IOException {
		byte[] barray = new byte[Constants.MPEGTS_PACKET_SIZE];
		if (inputStream.read(barray) != Constants.MPEGTS_PACKET_SIZE) {
			inputStream.close();
			return null;
		}

		// Parse the packet
		return new MTSPacket(ByteBuffer.wrap(barray));
	}

	public static InputStreamMTSSourceBuilder builder() {
		return new InputStreamMTSSourceBuilder();
	}

	public static class InputStreamMTSSourceBuilder {
		private InputStream inputStream;

		private InputStreamMTSSourceBuilder() {
		}

		public InputStreamMTSSourceBuilder setInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
			return this;
		}

		public InputStreamMTSSource build() throws IOException {
			Preconditions.checkNotNull(inputStream, "InputStream cannot be null");
			return new InputStreamMTSSource(inputStream);
		}
	}
}
