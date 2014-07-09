package org.taktik.mpegts;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class MultiMTSSource implements MTSSource {

	private MTSSource[] sources;
	int idx = 0;

	private Map<Integer,MTSPacket> pcrPackets = Maps.newHashMap();
	private Map<Integer,MTSPacket> allPackets = Maps.newHashMap();
	private Map<Integer,Long> ptss = Maps.newHashMap();
	private Map<Integer,Long> lastPTSsOfPreviousSource = Maps.newHashMap();
	private Map<Integer,Long> lastPCRsOfPreviousSource = Maps.newHashMap();
	private Map<Integer,Long> firstPCRsOfCurrentSource = Maps.newHashMap();
	private Map<Integer,Long> firstPTSsOfCurrentSource = Maps.newHashMap();

	private Map<Integer,MTSPacket> lastPacketsOfPreviousSource = Maps.newHashMap();
	private Map<Integer,MTSPacket> firstPacketsOfCurrentSource = Maps.newHashMap();
	private Map<Integer, Integer> continuityFixes = Maps.newHashMap();;


	private Queue<MTSPacket> queue = Lists.newLinkedList();

	protected MultiMTSSource(MTSSource... sources) {
		this.sources = sources;
	}

	protected MultiMTSSource(Collection<MTSSource> sources) {
		this.sources = sources.toArray(new MTSSource[sources.size()]);
	}

	private void switchSource() {
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

	@Override
	public MTSPacket nextPacket() throws Exception {
		if (!queue.isEmpty()) {
			return queue.remove();
		}
		if (idx >= sources.length) {
			return null;
		}
		MTSPacket tsPacket = sources[idx].nextPacket();
		if (tsPacket != null) {
			int pid = tsPacket.getPid();
			allPackets.put(pid, tsPacket);
			if (!firstPacketsOfCurrentSource.containsKey(pid)) {
				firstPacketsOfCurrentSource.put(pid, tsPacket);
				if (idx > 0) {
					int continuityFix = lastPacketsOfPreviousSource.get(pid).getContinuityCounter() - tsPacket.getContinuityCounter();
					if (tsPacket.isContainsPayload()) {
						continuityFix ++;
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
							long newPts = lastPTSsOfPreviousSource.get(pid) + (pts - firstPTSsOfCurrentSource.get(pid));

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

			return tsPacket;
		} else {
			idx++;
			if (idx < sources.length) {
				switchSource();
			}

			return nextPacket();
		}
	}

	private void rewritePCR(MTSPacket tsPacket) {
		if (idx == 0) {
			return;
		}
		Long lastPCROfPreviousSource = lastPCRsOfPreviousSource.get(tsPacket.getPid());
		Long firstPCROfCurrentSource = firstPCRsOfCurrentSource.get(tsPacket.getPid());
		long pcr = tsPacket.getAdaptationField().getPcr().getValue();

		long newPcr = lastPCROfPreviousSource + (pcr - firstPCROfCurrentSource);
		System.out.println("NewPcr : " + newPcr);
		tsPacket.getAdaptationField().getPcr().setValue(newPcr);
	}
}
