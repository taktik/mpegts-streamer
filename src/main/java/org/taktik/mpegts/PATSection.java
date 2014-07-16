package org.taktik.mpegts;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public class PATSection extends PSISection {
	private Integer[] networkPids;
	private Map<Integer, Integer> programs;

	public PATSection(PSISection psi, Integer[] networkPids, Map<Integer, Integer> programs) {
		super(psi);
		this.networkPids = networkPids;
		this.programs = programs;
	}

	public Integer[] getNetworkPids() {
		return networkPids;
	}

	public Map<Integer, Integer> getPrograms() {
		return programs;
	}

	public static PATSection parse(ByteBuffer data) {
		PSISection psi = PSISection.parse(data);
		if (psi == null) {
			return null;
		}
		List<Integer> networkPids = Lists.newArrayList();
		Map<Integer, Integer> programs = new HashMap<>();

		while (data.remaining() > 4) {
			int programNum = data.getShort() & 0xffff;
			int w = data.getShort();
			int pid = w & 0x1fff;
			if (programNum == 0)
				networkPids.add(pid);
			else
				programs.put(programNum, pid);
		}

		return new PATSection(psi, networkPids.toArray(new Integer[networkPids.size()]), programs);
	}
}