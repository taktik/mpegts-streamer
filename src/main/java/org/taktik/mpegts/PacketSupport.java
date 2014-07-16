package org.taktik.mpegts;


import java.nio.ByteBuffer;

public abstract class PacketSupport {
	protected ByteBuffer buffer;
	protected boolean dirty;

	public PacketSupport() {
		dirty = false;
	}

	public PacketSupport(ByteBuffer buffer) {
		this.buffer = buffer;
		buffer.rewind();
		parse();
		buffer.rewind();
		dirty = false;
	}

	public ByteBuffer getBuffer() {
		if (dirty) {
			write();
			buffer.rewind();
			dirty = false;
		}
		return buffer;
	}

	protected abstract void parse();
	protected abstract void write();

	protected void markDirty() {
		dirty = true;
	}
}
