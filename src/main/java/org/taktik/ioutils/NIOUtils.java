package org.taktik.ioutils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class NIOUtils {

	public static int read(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
		int rem = buffer.position();
		while (channel.read(buffer) != -1 && buffer.hasRemaining()) {
		}
		return buffer.position() - rem;
	}

	public static int skip(ByteBuffer buffer, int count) {
		int toSkip = Math.min(buffer.remaining(), count);
		buffer.position(buffer.position() + toSkip);
		return toSkip;
	}
}