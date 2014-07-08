package org.taktik.mpegts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.google.common.base.Preconditions;
import org.taktik.ioutils.NIOUtils;

public class MTSPacket {
	public boolean transportErrorIndicator;				// Transport Error Indicator (TEI)
	public boolean payloadUnitStartIndicator;			// Payload Unit Start Indicator
	public boolean transportPriority;					// Transport Priority
	public int pid;										// Packet Identifier (PID)
	public int scramblingControl;						// Scrambling control
	public boolean adaptationFieldExist;				// Adaptation field exist
	public boolean containsPayload;						// Contains payload
	public int continuityCounter;						// Continuity counter

	public AdaptationField adaptationField;
	public ByteBuffer payload;
	public ByteBuffer packet;

	public static class AdaptationField {
		public boolean discontinuityIndicator;				// Discontinuity indicator
		public boolean randomAccessIndicator;				// Random Access indicator
		public boolean elementaryStreamPriorityIndicator;	// Elementary stream priority indicator
		public boolean pcrFlag;								// PCR flag
		public boolean opcrFlag;							// OPCR flag
		public boolean splicingPointFlag;					// Splicing point flag
		public boolean transportPrivateDataFlag;			// Transport private data flag
		public boolean adaptationFieldExtensionFlag;		// Adaptation field extension flag
		public PCR pcr;										// PCR
		public PCR opcr;									// OPCR
		public byte spliceCountdown;						// Splice countdown

		public static class PCR {
			public long base;								// 33 bits
			public int extension;							// 9 bits

			public PCR(long base, int extension) {
				this.base = base;
				this.extension = extension;
			}

			public long getValue() {
				return base * 300 + extension;
			}
		}

		public AdaptationField(boolean discontinuityIndicator, boolean randomAccessIndicator, boolean elementaryStreamPriorityIndicator, boolean pcrFlag, boolean opcrFlag, boolean splicingPointFlag, boolean transportPrivateDataFlag, boolean adaptationFieldExtensionFlag, PCR pcr, PCR opcr, byte spliceCountdown) {
			this.discontinuityIndicator = discontinuityIndicator;
			this.randomAccessIndicator = randomAccessIndicator;
			this.elementaryStreamPriorityIndicator = elementaryStreamPriorityIndicator;
			this.pcrFlag = pcrFlag;
			this.opcrFlag = opcrFlag;
			this.splicingPointFlag = splicingPointFlag;
			this.transportPrivateDataFlag = transportPrivateDataFlag;
			this.adaptationFieldExtensionFlag = adaptationFieldExtensionFlag;
			this.pcr = pcr;
			this.opcr = opcr;
			this.spliceCountdown = spliceCountdown;
		}
	}

	protected MTSPacket(boolean transportErrorIndicator, boolean payloadUnitStartIndicator, boolean transportPriority, int pid, int scramblingControl, boolean adaptationFieldExist, boolean containsPayload, int continuityCounter, AdaptationField adaptationField, ByteBuffer packet, ByteBuffer payload) {
		this.transportErrorIndicator = transportErrorIndicator;
		this.payloadUnitStartIndicator = payloadUnitStartIndicator;
		this.transportPriority = transportPriority;
		this.pid = pid;
		this.scramblingControl = scramblingControl;
		this.adaptationFieldExist = adaptationFieldExist;
		this.containsPayload = containsPayload;
		this.continuityCounter = continuityCounter;
		this.adaptationField = adaptationField;
		this.payload = payload;
		this.packet = packet;
	}

	public static MTSPacket readPacket(ReadableByteChannel channel) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(188);
		if (NIOUtils.read(channel, buffer) != 188)
			return null;
		buffer.flip();
		return parsePacket(buffer);
	}

	public static MTSPacket parsePacket(ByteBuffer buffer) {
		ByteBuffer packet = buffer.slice();

		// Sync byte
		int marker = buffer.get() & 0xff;
		Preconditions.checkArgument(0x47 == marker);

		// Second/Third byte
		int bcBytes = buffer.getShort();

		// Fourth byte
		int dByte = buffer.get() & 0xff;

		// Transport Error Indicator (TEI)
		boolean transportErrorIndicator = (bcBytes & 0x8000) != 0;

		// Payload Unit Start Indicator
		boolean payloadUnitStartIndicator = (bcBytes & 0x4000) != 0;

		// Transport Priority
		boolean transportPriority = (bcBytes & 0x2000) != 0;

		// Packet Identifier (PID)
		int pid = bcBytes & 0x1fff;

		// Scrambling control
		int scramblingControl = dByte & 0xc0;

		// Adaptation field exist
		boolean adaptationFieldExist = (dByte & 0x20) != 0;

		// Contains payload
		boolean containsPayload = (dByte & 0x10) != 0;

		// Continuity counter
		int continuityCounter = dByte & 0x0f;

		MTSPacket.AdaptationField adaptationField = null;
		if (adaptationFieldExist) {
			// Adaptation Field Length
			int adaptationFieldLength = buffer.get() & 0xff;
			int remainingBytes = adaptationFieldLength;

			// Get next byte
			int nextByte = buffer.get() & 0xff;
			remainingBytes--;

			// Discontinuity indicator
			boolean discontinuityIndicator = (nextByte & 0x80) != 0;

			// Random Access indicator
			boolean randomAccessIndicator = (nextByte & 0x40) != 0;

			// Elementary stream priority indicator
			boolean elementaryStreamPriorityIndicator = (nextByte & 0x20) != 0;

			// PCR flag
			boolean pcrFlag = (nextByte & 0x10) != 0;

			// OPCR flag
			boolean opcrFlag = (nextByte & 0x08) != 0;

			// Splicing point flag
			boolean splicingPointFlag = (nextByte & 0x04) != 0;

			// Transport private data flag
			boolean transportPrivateDataFlag = (nextByte & 0x2) != 0;

			// Adaptation field extension flag
			boolean adaptationFieldExtensionFlag = (nextByte & 0x01) != 0;

			// PCR
			MTSPacket.AdaptationField.PCR pcr = null;
			if (pcrFlag) {
				byte[] pcrBytes = new byte[6];
				buffer.get(pcrBytes);
				remainingBytes -= pcrBytes.length;

				long pcrBits = ((pcrBytes[0] & 0xffL) << 40) | ((pcrBytes[1] & 0xffL) << 32) | ((pcrBytes[2] & 0xffL) << 24) | ((pcrBytes[3] & 0xffL) << 16) | ((pcrBytes[4] & 0xffL) << 8) | (pcrBytes[5] & 0xffL);
				long base = (pcrBits & 0xFFFFFFFF8000L) >> 15;
				int extension = (int) (pcrBits & 0x1FFL);
				pcr = new MTSPacket.AdaptationField.PCR(base, extension);
			}

			// OPCR
			MTSPacket.AdaptationField.PCR opcr = null;
			if (opcrFlag) {
				byte[] opcrBytes = new byte[6];
				buffer.get(opcrBytes);
				remainingBytes -= opcrBytes.length;

				long pcrBits = ((opcrBytes[0] & 0xffL) << 40) | ((opcrBytes[1] & 0xffL) << 32) | ((opcrBytes[2] & 0xffL) << 24) | ((opcrBytes[3] & 0xffL) << 16) | ((opcrBytes[4] & 0xffL) << 8) | (opcrBytes[5] & 0xffL);
				long base = (pcrBits & 0xFFFFFFFF8000L) >> 15;
				int extension = (int) (pcrBits & 0x1FFL);

				opcr = new MTSPacket.AdaptationField.PCR(base, extension);
			}

			// Splice countdown
			byte spliceCountdown = 0;
			if (splicingPointFlag) {
				spliceCountdown = buffer.get();
				remainingBytes--;
			}

			// Skip remaining bytes
			NIOUtils.skip(buffer, remainingBytes);

			adaptationField = new MTSPacket.AdaptationField(discontinuityIndicator, randomAccessIndicator, elementaryStreamPriorityIndicator, pcrFlag, opcrFlag, splicingPointFlag, transportPrivateDataFlag, adaptationFieldExtensionFlag, pcr, opcr, spliceCountdown);
		}

		// Payload
		ByteBuffer payload = containsPayload ? buffer.slice() : null;

		return new MTSPacket(transportErrorIndicator, payloadUnitStartIndicator, transportPriority, pid, scramblingControl, adaptationFieldExist, containsPayload, continuityCounter, adaptationField, packet, payload);
	}
}