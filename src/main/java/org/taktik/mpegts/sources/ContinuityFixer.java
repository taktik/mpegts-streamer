package org.taktik.mpegts.sources;

import java.nio.ByteBuffer;
import java.util.Map;

import com.google.common.collect.Maps;
import org.taktik.mpegts.MTSPacket;

public class ContinuityFixer {
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

	boolean firstSource;


	public ContinuityFixer() {
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
		firstSource = true;
	}

	public void nextSource() {
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
		firstSource = false;
	}

	public void fixContinuity(MTSPacket tsPacket) {
		int pid = tsPacket.getPid();
		allPackets.put(pid, tsPacket);
		if (!firstPacketsOfCurrentSource.containsKey(pid)) {
			firstPacketsOfCurrentSource.put(pid, tsPacket);
			if (!firstSource) {
				MTSPacket lastPacketOfPreviousSource = lastPacketsOfPreviousSource.get(pid);
				int continuityFix = lastPacketOfPreviousSource == null ? 0 : lastPacketOfPreviousSource.getContinuityCounter() - tsPacket.getContinuityCounter();
				if (tsPacket.isContainsPayload()) {
					continuityFix++;
				}
				continuityFixes.put(pid, continuityFix);
			}
		}
		if (!firstSource) {
			tsPacket.setContinuityCounter((tsPacket.getContinuityCounter() + continuityFixes.get(pid)) % 16);
		}
		fixPTS(tsPacket, pid);
		fixPCR(tsPacket, pid);
	}

	private void fixPCR(MTSPacket tsPacket, int pid) {
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

	private void fixPTS(MTSPacket tsPacket, int pid) {
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
					if (!firstSource) {
						Long lastPTSOfPreviousSource = lastPTSsOfPreviousSource.get(pid);
						if (lastPTSOfPreviousSource == null) {
							lastPTSOfPreviousSource = 0l;
						}
						long newPts = lastPTSOfPreviousSource + (pts - firstPTSsOfCurrentSource.get(pid)) + 100 * ((27000000 / 300) / 1000);

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
	}

	private void rewritePCR(MTSPacket tsPacket) {
		if (firstSource) {
			return;
		}
		Long lastPCROfPreviousSource = lastPCRsOfPreviousSource.get(tsPacket.getPid());
		if (lastPCROfPreviousSource == null) {
			lastPCROfPreviousSource = 0l;
		}
		Long firstPCROfCurrentSource = firstPCRsOfCurrentSource.get(tsPacket.getPid());
		long pcr = tsPacket.getAdaptationField().getPcr().getValue();

		long newPcr = lastPCROfPreviousSource + (pcr - firstPCROfCurrentSource) + 100 * ((27000000) / 1000);
		System.out.println("NewPcr : " + newPcr);
		tsPacket.getAdaptationField().getPcr().setValue(newPcr);
	}
}
