package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

public abstract class AbstractMTSSource implements MTSSource {
	private boolean closed;

	@Override
	public final MTSPacket nextPacket() throws Exception {
		if (closed) {
			throw new IllegalStateException("Source is closed");
		}
		return nextPacketInternal();
	}

	@Override
	public final void close() throws Exception {
		try {
			closeInternal();
		} finally {
			closed = true;
		}
	}

	protected boolean isClosed() {
		return closed;
	}

	protected abstract MTSPacket nextPacketInternal() throws Exception;
	protected abstract void closeInternal() throws Exception;


	protected void finalize() throws Exception {
		if (!closed) {
			close();
		}
	}
}
