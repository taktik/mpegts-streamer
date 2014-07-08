package org.taktik.mpegts;

import java.util.Collection;

class MultiMTSSource implements MTSSource {

	private MTSSource[] sources;
	int idx = 0;

	protected MultiMTSSource(MTSSource... sources) {
		this.sources = sources;
	}

	protected MultiMTSSource(Collection<MTSSource> sources) {
		this.sources = sources.toArray(new MTSSource[sources.size()]);
	}

	 @Override
	 public MTSPacket nextPacket() throws Exception {
		 if (idx >= sources.length) {
			 return null;
		 }
		 MTSPacket next = sources[idx].nextPacket();
		 if (next != null) {
			 return next;
		 } else {
			 idx ++;
			 return nextPacket();
		 }
	 }
 }
