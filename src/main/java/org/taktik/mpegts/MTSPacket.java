package org.taktik.mpegts;

import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;
import org.taktik.ioutils.NIOUtils;

public class MTSPacket extends PacketSupport {
	private boolean transportErrorIndicator;				// Transport Error Indicator (TEI)
	private boolean payloadUnitStartIndicator;			// Payload Unit Start Indicator
	private boolean transportPriority;					// Transport Priority
	private int pid;										// Packet Identifier (PID)
	private int scramblingControl;						// Scrambling control
	private boolean adaptationFieldExist;				// Adaptation field exist
	private boolean containsPayload;						// Contains payload
	private int continuityCounter;						// Continuity counter

	private AdaptationField adaptationField;
	private ByteBuffer payload;

	public static class AdaptationField {
		private MTSPacket packet;
		private boolean discontinuityIndicator;				// Discontinuity indicator
		private boolean randomAccessIndicator;				// Random Access indicator
		private boolean elementaryStreamPriorityIndicator;	// Elementary stream priority indicator
		private boolean pcrFlag;								// PCR flag
		private boolean opcrFlag;							// OPCR flag
		private boolean splicingPointFlag;					// Splicing point flag
		private boolean transportPrivateDataFlag;			// Transport private data flag
		private boolean adaptationFieldExtensionFlag;		// Adaptation field extension flag
		private PCR pcr;										// PCR
		private PCR opcr;									// OPCR
		private byte spliceCountdown;						// Splice countdown
		private byte[] privateData;                          // Private data
		private byte[] extension;                            // Extension

		public static class PCR {
			private AdaptationField field;
			public long base;								// 33 bits
			public int extension;							// 9 bits
			public byte reserved;                           // 6 bits

			public PCR(AdaptationField field, long base, int extension, byte reserved) {
				this.base = base;
				this.extension = extension;
				this.reserved = reserved;
				this.field = field;
			}

			public long getValue() {
				return base * 300 + extension;
			}

			public void setValue(long value) {
				base = value / 300;
				extension = (int)value % 300;
				field.markDirty();
			}

			public void write(ByteBuffer buffer) {
				buffer.putInt((int) ((base & 0x1FFFFFFFFL) >> 1));
				int middleByte = 0;
				middleByte |= ((base & 0x1) << 7);
				middleByte |= ((reserved & 0x3F) << 1);
				middleByte |= ((extension & 0x1FF) >> 8);
				buffer.put((byte) middleByte);
				buffer.put((byte) (extension & 0xff));
			}
		}

		public AdaptationField(MTSPacket packet, boolean discontinuityIndicator, boolean randomAccessIndicator, boolean elementaryStreamPriorityIndicator, boolean pcrFlag, boolean opcrFlag, boolean splicingPointFlag, boolean transportPrivateDataFlag, boolean adaptationFieldExtensionFlag, PCR pcr, PCR opcr, byte spliceCountdown, byte[] privateData, byte[] extension) {
			this.packet = packet;
			this.discontinuityIndicator = discontinuityIndicator;
			this.randomAccessIndicator = randomAccessIndicator;
			this.elementaryStreamPriorityIndicator = elementaryStreamPriorityIndicator;
			this.pcrFlag = pcrFlag;
			this.opcrFlag = opcrFlag;
			this.splicingPointFlag = splicingPointFlag;
			this.transportPrivateDataFlag = transportPrivateDataFlag;
			this.adaptationFieldExtensionFlag = adaptationFieldExtensionFlag;
			this.pcr = pcr;
			if (this.pcr != null) {
				this.pcr.field = this;
			}
			this.opcr = opcr;
			if (this.opcr != null) {
				this.opcr.field = this;
			}
			this.spliceCountdown = spliceCountdown;
			this.privateData = privateData;
			this.extension = extension;
		}

		public boolean isDiscontinuityIndicator() {
			return discontinuityIndicator;
		}

		public void setDiscontinuityIndicator(boolean discontinuityIndicator) {
			this.discontinuityIndicator = discontinuityIndicator;
			markDirty();
		}

		private void markDirty() {
			packet.markDirty();
		}

		public boolean isRandomAccessIndicator() {
			return randomAccessIndicator;
		}

		public void setRandomAccessIndicator(boolean randomAccessIndicator) {
			this.randomAccessIndicator = randomAccessIndicator;
			markDirty();
		}

		public boolean isElementaryStreamPriorityIndicator() {
			return elementaryStreamPriorityIndicator;
		}

		public void setElementaryStreamPriorityIndicator(boolean elementaryStreamPriorityIndicator) {
			this.elementaryStreamPriorityIndicator = elementaryStreamPriorityIndicator;
			markDirty();
		}

		public boolean isPcrFlag() {
			return pcrFlag;
		}

		public void setPcrFlag(boolean pcrFlag) {
			this.pcrFlag = pcrFlag;
			markDirty();
		}

		public boolean isOpcrFlag() {
			return opcrFlag;
		}

		public void setOpcrFlag(boolean opcrFlag) {
			this.opcrFlag = opcrFlag;
			markDirty();
		}

		public boolean isSplicingPointFlag() {
			return splicingPointFlag;
		}

		public void setSplicingPointFlag(boolean splicingPointFlag) {
			this.splicingPointFlag = splicingPointFlag;
			markDirty();
		}

		public boolean isTransportPrivateDataFlag() {
			return transportPrivateDataFlag;
		}

		public void setTransportPrivateDataFlag(boolean transportPrivateDataFlag) {
			this.transportPrivateDataFlag = transportPrivateDataFlag;
			markDirty();
		}

		public boolean isAdaptationFieldExtensionFlag() {
			return adaptationFieldExtensionFlag;
		}

		public void setAdaptationFieldExtensionFlag(boolean adaptationFieldExtensionFlag) {
			this.adaptationFieldExtensionFlag = adaptationFieldExtensionFlag;
			markDirty();
		}

		public PCR getPcr() {
			return pcr;
		}

		public void setPcr(PCR pcr) {
			this.pcr = pcr;
			markDirty();
		}

		public PCR getOpcr() {
			return opcr;
		}

		public void setOpcr(PCR opcr) {
			this.opcr = opcr;
			markDirty();
		}

		public byte getSpliceCountdown() {
			return spliceCountdown;
		}

		public void setSpliceCountdown(byte spliceCountdown) {
			this.spliceCountdown = spliceCountdown;
			markDirty();
		}

		public byte[] getPrivateData() {
			return privateData;
		}

		public void setPrivateData(byte[] privateData) {
			this.privateData = privateData;
			markDirty();
		}

		public byte[] getExtension() {
			return extension;
		}

		public void setExtension(byte[] extension) {
			this.extension = extension;
			markDirty();
		}

		public void write(ByteBuffer buffer, int payloadLength) {
			int length = 183 - payloadLength;
			int remaining = length;
			buffer.put((byte) (length & 0xff));
			int firstByte = 0;

			// Discontinuity indicator
			if (discontinuityIndicator) {
				firstByte |= 0x80;
			}

			// Random Access indicator
			if (randomAccessIndicator) {
				firstByte |= 0x40;
			}

			// Elementary stream priority indicator
			if (elementaryStreamPriorityIndicator) {
				firstByte |= 0x20;
			}

			// PCR flag
			if (pcrFlag) {
				firstByte |= 0x10;
			}

			// OPCR flag
			if (opcrFlag) {
				firstByte |= 0x08;
			}

			// Splicing point flag
			if (splicingPointFlag) {
				firstByte |= 0x04;
			}

			// Transport private data flag
			if (transportPrivateDataFlag) {
				firstByte |= 0x2;
			}

			// Adaptation field extension flag
			if (adaptationFieldExtensionFlag) {
				firstByte |= 0x01;
			}

			buffer.put((byte) firstByte);
			remaining --;

			if (pcrFlag && pcr != null) {
				pcr.write(buffer);
				remaining -= 6;
			}

			if (opcrFlag && opcr != null) {
				opcr.write(buffer);
				remaining -= 6;
			}

			if (splicingPointFlag) {
				buffer.put(spliceCountdown);
				remaining--;
			}

			if (transportPrivateDataFlag && privateData != null) {
				buffer.put(privateData);
				remaining -= privateData.length;
			}

			if (adaptationFieldExtensionFlag && extension != null) {
				buffer.put(extension);
				remaining -= extension.length;
			}

			if (remaining < 0) {
				throw new IllegalStateException("Adaptation field too big!");
			}
			while (remaining-- > 0) {
				buffer.put((byte)0);
			}
		}
	}

	public MTSPacket(boolean transportErrorIndicator, boolean payloadUnitStartIndicator, boolean transportPriority, int pid, int scramblingControl, int continuityCounter) {
		super();
		this.buffer = ByteBuffer.allocate(Constants.MPEGTS_PACKET_SIZE);
		this.transportErrorIndicator = transportErrorIndicator;
		this.payloadUnitStartIndicator = payloadUnitStartIndicator;
		this.transportPriority = transportPriority;
		this.pid = pid;
		this.scramblingControl = scramblingControl;
		this.continuityCounter = continuityCounter;
		this.adaptationFieldExist = false;
		this.containsPayload = false;
	}

	public MTSPacket(ByteBuffer buffer) {
		super(buffer);
	}

	@Override
	protected void write() {
		// First write payload
		int payloadLength = 0;
		if (containsPayload && payload != null) {
			payloadLength = payload.capacity();
			buffer.mark();
			payload.mark();
			buffer.position(Constants.MPEGTS_PACKET_SIZE - payloadLength);
			buffer.put(payload);
			buffer.reset();
			payload.reset();
		}

		// First byte
		buffer.put((byte) 0x47);

		// Bytes 2->3
		int secondAndThirdBytes = 0;
		if (transportErrorIndicator) {
			secondAndThirdBytes |= 0x8000;
		}
		if (payloadUnitStartIndicator) {
			secondAndThirdBytes |= 0x4000;
		}
		if (transportPriority) {
			secondAndThirdBytes |= 0x2000;
		}
		secondAndThirdBytes |= (pid & 0x1fff);

		buffer.putShort((short) secondAndThirdBytes);

		int fourthByte = 0;

		// Byte 4
		fourthByte |= (scramblingControl & 0xc0);
		if (adaptationFieldExist) {
			fourthByte |= 0x20;
		}
		if (containsPayload) {
			fourthByte |= 0x10;
		}
		fourthByte |= (continuityCounter & 0x0f);
		buffer.put((byte) fourthByte);

		// Adaptation field
		if (adaptationFieldExist) {
			if (adaptationField == null) {
				buffer.put((byte) 0);
			} else {
				adaptationField.write(buffer, payloadLength);
			}
		}

		// Payload
		if (containsPayload && payload != null) {
			buffer.put(payload);
		}
		if (buffer.remaining() != 0) {
			throw new IllegalStateException("buffer.remaining() = " + buffer.remaining() + ", should be zero!");
		}
	}

	protected void parse() {
		// Sync byte
		int marker = buffer.get() & 0xff;
		Preconditions.checkArgument(0x47 == marker);

		// Second/Third byte
		int secondAndThirdBytes = buffer.getShort();

		// Transport Error Indicator (TEI)
		boolean transportErrorIndicator = (secondAndThirdBytes & 0x8000) != 0;

		// Payload Unit Start Indicator
		boolean payloadUnitStartIndicator = (secondAndThirdBytes & 0x4000) != 0;

		// Transport Priority
		boolean transportPriority = (secondAndThirdBytes & 0x2000) != 0;

		// Packet Identifier (PID)
		int pid = secondAndThirdBytes & 0x1fff;

		// Fourth byte
		int fourthByte = buffer.get() & 0xff;

		// Scrambling control
		int scramblingControl = fourthByte & 0xc0;

		// Adaptation field exist
		boolean adaptationFieldExist = (fourthByte & 0x20) != 0;

		// Contains payload
		boolean containsPayload = (fourthByte & 0x10) != 0;

		// Continuity counter
		int continuityCounter = fourthByte & 0x0f;

		MTSPacket.AdaptationField adaptationField = null;
		if (adaptationFieldExist) {
			// Adaptation Field Length
			int adaptationFieldLength = buffer.get() & 0xff;
			if (adaptationFieldLength != 0) {

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
					pcr = parsePCR();
					remainingBytes -= 6;
				}

				// OPCR
				MTSPacket.AdaptationField.PCR opcr = null;
				if (opcrFlag) {
					opcr = parsePCR();
					remainingBytes -= 6;
				}

				// Splice countdown
				byte spliceCountdown = 0;
				if (splicingPointFlag) {
					spliceCountdown = buffer.get();
					remainingBytes--;
				}

				byte[] privateData = null;
				if (transportPrivateDataFlag) {
					int transportPrivateDataLength = buffer.get() & 0xff;
					privateData = new byte[transportPrivateDataLength];
					buffer.get(privateData);
					remainingBytes -= transportPrivateDataLength;
				}

				byte[] extension = null;
				if (adaptationFieldExtensionFlag) {
					int extensionLength = buffer.get() & 0xff;
					extension = new byte[extensionLength];
					buffer.get(extension);
					remainingBytes -= extensionLength;
				}

				// Skip remaining bytes
				NIOUtils.skip(buffer, remainingBytes);

				adaptationField = new MTSPacket.AdaptationField(this, discontinuityIndicator, randomAccessIndicator, elementaryStreamPriorityIndicator, pcrFlag, opcrFlag, splicingPointFlag, transportPrivateDataFlag, adaptationFieldExtensionFlag, pcr, opcr, spliceCountdown, privateData, extension);
			}
		}

		this.transportErrorIndicator = transportErrorIndicator;
		this.payloadUnitStartIndicator = payloadUnitStartIndicator;
		this.transportPriority = transportPriority;
		this.pid = pid;
		this.scramblingControl = scramblingControl;
		this.adaptationFieldExist = adaptationFieldExist;
		this.containsPayload = containsPayload;
		this.continuityCounter = continuityCounter;
		this.adaptationField = adaptationField;

		// Payload
		this.payload = containsPayload ? buffer.slice() : null;
	}

	private AdaptationField.PCR parsePCR() {
		AdaptationField.PCR pcr;
		byte[] pcrBytes = new byte[6];
		buffer.get(pcrBytes);

		long pcrBits = ((pcrBytes[0] & 0xffL) << 40) | ((pcrBytes[1] & 0xffL) << 32) | ((pcrBytes[2] & 0xffL) << 24) | ((pcrBytes[3] & 0xffL) << 16) | ((pcrBytes[4] & 0xffL) << 8) | (pcrBytes[5] & 0xffL);
		long base = (pcrBits & 0xFFFFFFFF8000L) >> 15;
		byte reserved =  (byte) ((pcrBits & 0x7E00) >> 9);
		int extension = (int) (pcrBits & 0x1FFL);
		pcr = new AdaptationField.PCR(null, base, extension, reserved);
		return pcr;
	}

	public boolean isTransportErrorIndicator() {
		return transportErrorIndicator;
	}

	public void setTransportErrorIndicator(boolean transportErrorIndicator) {
		this.transportErrorIndicator = transportErrorIndicator;
		markDirty();
	}

	public boolean isPayloadUnitStartIndicator() {
		return payloadUnitStartIndicator;
	}

	public void setPayloadUnitStartIndicator(boolean payloadUnitStartIndicator) {
		this.payloadUnitStartIndicator = payloadUnitStartIndicator;
		markDirty();
	}

	public boolean isTransportPriority() {
		return transportPriority;
	}

	public void setTransportPriority(boolean transportPriority) {
		this.transportPriority = transportPriority;
		markDirty();
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
		markDirty();
	}

	public int getScramblingControl() {
		return scramblingControl;
	}

	public void setScramblingControl(int scramblingControl) {
		this.scramblingControl = scramblingControl;
		markDirty();
	}

	public boolean isAdaptationFieldExist() {
		return adaptationFieldExist;
	}

	public void setAdaptationFieldExist(boolean adaptationFieldExist) {
		this.adaptationFieldExist = adaptationFieldExist;
		markDirty();
	}

	public boolean isContainsPayload() {
		return containsPayload;
	}

	public void setContainsPayload(boolean containsPayload) {
		this.containsPayload = containsPayload;
		markDirty();
	}

	public int getContinuityCounter() {
		return continuityCounter;
	}

	public void setContinuityCounter(int continuityCounter) {
		this.continuityCounter = continuityCounter;
		markDirty();
	}

	public AdaptationField getAdaptationField() {
		return adaptationField;
	}

	public void setAdaptationField(AdaptationField adaptationField) {
		this.adaptationField = adaptationField;
		markDirty();
	}

	public ByteBuffer getPayload() {
		return payload;
	}

	public void setPayload(ByteBuffer payload) {
		this.payload = payload;
		markDirty();
	}
}