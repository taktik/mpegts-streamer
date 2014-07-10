package org.taktik.mpegts.sources;

import org.taktik.mpegts.MTSPacket;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LoopingMTSSource implements MTSSource{
	private ResettableMTSSource source;
	private boolean fixContinuity;
	private Integer maxLoops;
	private long currentLoop;

	public LoopingMTSSource(ResettableMTSSource source, boolean fixContinuity, Integer maxLoops) {
		this.source = source;
		this.fixContinuity = fixContinuity;
		this.maxLoops = maxLoops;
		currentLoop = 1;
	}

	@Override
	public MTSPacket nextPacket() throws Exception {
		MTSPacket packet = source.nextPacket();
		if (packet == null) {
			currentLoop++;
			if (maxLoops == null || (currentLoop <= maxLoops)) {
				source.reset();
				packet = source.nextPacket();
			}
		}
		return packet;
	}

	public static LoopingMTSSourceBuilder builder() {
		return new LoopingMTSSourceBuilder();
	}

	public static class LoopingMTSSourceBuilder {
		private ResettableMTSSource source;
		private boolean fixContinuity;
		private Integer maxLoops;

		private LoopingMTSSourceBuilder(){}

		public LoopingMTSSource build() {
			checkNotNull(source);
			checkArgument(maxLoops == null || maxLoops > 0);
			return new LoopingMTSSource(source, fixContinuity, maxLoops);
		}

		public LoopingMTSSourceBuilder setSource(ResettableMTSSource source) {
			this.source = source;
			return this;
		}

		public LoopingMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
			this.fixContinuity = fixContinuity;
			return this;
		}

		public LoopingMTSSourceBuilder setMaxLoops(Integer maxLoops) {
			this.maxLoops = maxLoops;
			return this;
		}
	}
}
