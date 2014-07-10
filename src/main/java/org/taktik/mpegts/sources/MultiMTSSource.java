package org.taktik.mpegts.sources;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.taktik.mpegts.MTSPacket;

public class MultiMTSSource implements MTSSource {

	boolean fixContinuity;
	private MTSSource[] sources;
	private MTSSource currentSource;
	int idx;

	private Map<Integer,MTSPacket> pcrPackets;
	private Map<Integer,MTSPacket> allPackets;
	private Map<Integer,Long> ptss;
	private Map<Integer,Long> lastPTSsOfPreviousSource;
	private Map<Integer,Long> lastPCRsOfPreviousSource;
	private Map<Integer,Long> firstPCRsOfCurrentSource;
	private Map<Integer,Long> firstPTSsOfCurrentSource;

	private Map<Integer,MTSPacket> lastPacketsOfPreviousSource = Maps.newHashMap();
	private Map<Integer,MTSPacket> firstPacketsOfCurrentSource = Maps.newHashMap();
	private Map<Integer, Integer> continuityFixes = Maps.newHashMap();

	protected MultiMTSSource(boolean fixContinuity, MTSSource... sources) {
		Preconditions.checkArgument(sources.length > 0, "Multisource must at least contain one source");
		this.sources = sources;
		this.fixContinuity = fixContinuity;
		idx = 0;
		currentSource = this.sources[0];
		if (fixContinuity) {
			pcrPackets = Maps.newHashMap();
			allPackets = Maps.newHashMap();
			ptss = Maps.newHashMap();
			lastPTSsOfPreviousSource = Maps.newHashMap();
			lastPCRsOfPreviousSource = Maps.newHashMap();
			firstPCRsOfCurrentSource = Maps.newHashMap();
			firstPTSsOfCurrentSource = Maps.newHashMap();

			lastPacketsOfPreviousSource = Maps.newHashMap();
			firstPacketsOfCurrentSource = Maps.newHashMap();
			continuityFixes = Maps.newHashMap();
		}
	}

	protected MultiMTSSource(boolean fixContinuity, Collection<MTSSource> sources) {
		this(fixContinuity, sources.toArray(new MTSSource[sources.size()]));
	}

	@Override
	public MTSPacket nextPacket() throws Exception {
		if (currentSource == null) {
			return null;
		}
		MTSPacket tsPacket = sources[idx].nextPacket();
		if (tsPacket != null) {
			if (fixContinuity) {
				fixContinuity(tsPacket);
			}
			return tsPacket;
		} else {
			switchSource();
			return nextPacket();
		}
	}

	private void switchSource() {
		if (fixContinuity) {
			firstPCRsOfCurrentSource.clear();
			lastPCRsOfPreviousSource.clear();
			firstPTSsOfCurrentSource.clear();
			lastPTSsOfPreviousSource.clear();
			firstPacketsOfCurrentSource.clear();
			lastPacketsOfPreviousSource.clear();
			for (MTSPacket mtsPacket : pcrPackets.values()) {
				lastPCRsOfPreviousSource.put(mtsPacket.getPid(), mtsPacket.getAdaptationField().getPcr().getValue());
			}
			lastPTSsOfPreviousSource.putAll(ptss);
			lastPacketsOfPreviousSource.putAll(allPackets);
			pcrPackets.clear();
			ptss.clear();
			allPackets.clear();
		}
		idx++;
		currentSource = (idx < sources.length) ? sources[idx] : null;
	}

	private void fixContinuity(MTSPacket tsPacket) {
		int pid = tsPacket.getPid();
		allPackets.put(pid, tsPacket);
		if (!firstPacketsOfCurrentSource.containsKey(pid)) {
			firstPacketsOfCurrentSource.put(pid, tsPacket);
			if (idx > 0) {
				MTSPacket lastPacketOfPreviousSource = lastPacketsOfPreviousSource.get(pid);
				int continuityFix = lastPacketOfPreviousSource == null ? 0 : lastPacketOfPreviousSource.getContinuityCounter() - tsPacket.getContinuityCounter();
				if (tsPacket.isContainsPayload()) {
					continuityFix++;
				}
				continuityFixes.put(pid, continuityFix);
			}
		}
		if (idx > 0) {
			tsPacket.setContinuityCounter((tsPacket.getContinuityCounter() + continuityFixes.get(pid)) % 16);
		}
		if (tsPacket.isContainsPayload()) {
			ByteBuffer payload = tsPacket.getPayload();
			if (((payload.get(0) & 0xff) == 0) && ((payload.get(1) & 0xff) == 0) && ((payload.get(2) & 0xff) == 1)) {
				int extension = payload.getShort(6) & 0xffff;
				if ((extension & 0x80) != 0) {
					// PTS is present
					long pts = (((payload.get(9) & 0xE)) << 29) | (((payload.getShort(10) & 0xFFFE)) << 14) | ((payload.getShort(12) & 0xFFFE) >> 1);
					if (!firstPTSsOfCurrentSource.containsKey(pid)) {
						firstPTSsOfCurrentSource.put(pid, pts);
					}
					if (idx > 0) {
						Long lastPTSOfPreviousSource = lastPTSsOfPreviousSource.get(pid);
						if (lastPTSOfPreviousSource == null) {
							lastPTSOfPreviousSource = 0l;
						}
						long newPts = lastPTSOfPreviousSource + (pts - firstPTSsOfCurrentSource.get(pid));// + 70 * ((27000000 / 300) / 1000);

						payload.put(9, (byte) (0x20 | ((newPts & 0x1C0000000l) >> 29) | 0x1));
						payload.putShort(10, (short) (0x1 | ((newPts & 0x3FFF8000) >> 14)));
						payload.putShort(12, (short) (0x1 | ((newPts & 0x7FFF) << 1)));
						payload.rewind();
						pts = newPts;
					}

					ptss.put(pid, pts);
				}
			}
		}
		if (tsPacket.isAdaptationFieldExist() && tsPacket.getAdaptationField() != null) {
			if (tsPacket.getAdaptationField().isPcrFlag()) {

				if (!firstPCRsOfCurrentSource.containsKey(pid)) {
					firstPCRsOfCurrentSource.put(pid, tsPacket.getAdaptationField().getPcr().getValue());
				}
				rewritePCR(tsPacket);


				pcrPackets.put(pid, tsPacket);
			}
		}
	}

	private void rewritePCR(MTSPacket tsPacket) {
		if (idx == 0) {
			return;
		}
		Long lastPCROfPreviousSource = lastPCRsOfPreviousSource.get(tsPacket.getPid());
		if (lastPCROfPreviousSource == null) {
			lastPCROfPreviousSource = 0l;
		}
		Long firstPCROfCurrentSource = firstPCRsOfCurrentSource.get(tsPacket.getPid());
		long pcr = tsPacket.getAdaptationField().getPcr().getValue();

		long newPcr = lastPCROfPreviousSource + (pcr - firstPCROfCurrentSource);//; + 70 * ((27000000) / 1000);;
		System.out.println("NewPcr : " + newPcr);
		tsPacket.getAdaptationField().getPcr().setValue(newPcr);
	}

	public static MultiMTSSourceBuilder builder() {
		return new MultiMTSSourceBuilder();
	}

	public static class MultiMTSSourceBuilder {
		private List<MTSSource> sources = Lists.newArrayList();
		boolean fixContinuity = false;

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

		public MultiMTSSource build() {
			return new MultiMTSSource(fixContinuity, sources);
		}
	}
}
