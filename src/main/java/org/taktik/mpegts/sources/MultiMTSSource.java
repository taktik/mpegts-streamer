package org.taktik.mpegts.sources;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.taktik.mpegts.MTSPacket;

public class MultiMTSSource implements MTSSource {

	private List<MTSSource> sources;
	private MTSSource currentSource;
	private int idx;
	private boolean fixContinuity;

	private ContinuityFixer continuityFixer;
	private int maxLoops;
	private int currentLoop;

	protected MultiMTSSource(boolean fixContinuity, int maxloops, Collection<MTSSource> sources) {
		Preconditions.checkArgument(sources.size() > 0, "Multisource must at least contain one source");
		Preconditions.checkArgument(maxloops != 0, "Cannot loop zero times");
		this.sources = Lists.newLinkedList(sources);
		this.fixContinuity = fixContinuity;
		idx = 0;
		currentSource = this.sources.get(0);
		if (fixContinuity) {
			continuityFixer = new ContinuityFixer();
		}
		this.maxLoops = maxloops;
		if (maxloops != 1) {
			checkLoopingPossible();
		}
		this.currentLoop = 1;
	}

	private void checkLoopingPossible() {
		for (MTSSource source : sources) {
			if (!(source instanceof ResettableMTSSource)) {
				throw new IllegalStateException("Sources must be resettable for looping");
			}
		}
	}

	@Override
	public MTSPacket nextPacket() throws Exception {
		if (currentSource == null) {
			return null;
		}
		MTSPacket tsPacket = currentSource.nextPacket();
		if (tsPacket != null) {
			if (fixContinuity) {
				continuityFixer.fixContinuity(tsPacket);
			}
			return tsPacket;
		} else {
			nextSource();
			return nextPacket();
		}
	}

	private void nextSource() {
		if (fixContinuity) {
			continuityFixer.nextSource();
		}
		idx++;
		if (idx >= sources.size()) {
			currentLoop++;
			if (maxLoops > 0 && currentLoop > maxLoops) {
				currentSource = null;
			} else {
				idx = 0;
				for (MTSSource source : sources) {
					try {
						((ResettableMTSSource)source).reset();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				currentSource = sources.get(idx);
			}
		} else {
			currentSource = sources.get(idx);
		}
	}


	public static MultiMTSSourceBuilder builder() {
		return new MultiMTSSourceBuilder();
	}

	public static class MultiMTSSourceBuilder {
		private List<MTSSource> sources = Lists.newArrayList();
		boolean fixContinuity = false;
		private int maxLoops = 1;

		private MultiMTSSourceBuilder(){}

		public MultiMTSSourceBuilder addSource(MTSSource source) {
			sources.add(source);
			return this;
		}

		public MultiMTSSourceBuilder addSources(Collection<MTSSource> sources) {
			sources.addAll(sources);
			return this;
		}

		public MultiMTSSourceBuilder setSources(MTSSource... sources) {
			this.sources = Lists.newArrayList(sources);
			return this;
		}

		public MultiMTSSourceBuilder setSources(Collection<MTSSource> sources) {
			this.sources = Lists.newArrayList(sources);
			return this;
		}

		public MultiMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
			this.fixContinuity = fixContinuity;
			return this;
		}

		public MultiMTSSourceBuilder loop() {
			this.maxLoops = -1;
			return this;
		}

		public MultiMTSSourceBuilder loop(int count) {
			this.maxLoops = count;
			return this;
		}

		public MultiMTSSourceBuilder noLoop() {
			this.maxLoops = 1;
			return this;
		}

		public MultiMTSSource build() {
			return new MultiMTSSource(fixContinuity,maxLoops, sources);
		}
	}
}
