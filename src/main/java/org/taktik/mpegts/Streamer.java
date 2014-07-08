package org.taktik.mpegts;

import com.google.common.base.Preconditions;

public class Streamer {
	private MTSSource source;
	private MTSSink sink;

	private Streamer(MTSSource source, MTSSink sink) {
		this.source = source;
		this.sink = sink;
	}

	public void stream() throws Exception {


			boolean resetState = false;

			Integer pcrPid = null;

			MTSPacket mtsPacket;
			long packetCount = 0;
			long pcrCount = 0;
			//long pcrPidPacketCount = 0;
			Long firstPcrValue = null;
			Long firstPcrTime = null;
			//Long firstPcrPacketCount = null;
			Long lastPcrValue = null;
			Long lastPcrTime = null;
			//Long lastPcrPacketCount = null;
			Long averageSleep = null;
			while (true) {

				if (resetState) {
					packetCount = 0;
					pcrCount = 0;
					firstPcrValue = null;
					firstPcrTime = null;
					lastPcrValue = null;
					lastPcrTime = null;
					averageSleep = null;
					resetState = false;
				}

				// Initialize time to sleep
				long sleepNanos = 0;
				mtsPacket = source.nextPacket();

				if (mtsPacket == null) {
					System.err.println("End of source reached");
					break;
				}

				// Check PID matches PCR PID
				if (true) {//mtsPacket.pid == pmt.getPcrPid()) {
					//pcrPidPacketCount++;

					if (averageSleep != null) {
						sleepNanos = averageSleep;
					} else {
//						if (pcrPidPacketCount < 2) {
//							if (pcrPidPacketCount % 10 == 0) {
//								sleepNanos = 15;
//							}
//						}
					}
				}

				// Check for PCR
				if (mtsPacket.adaptationField != null) {
					if (mtsPacket.adaptationField.pcr != null) {
						if (true) {//mtsPacket.pid == pmt.getPcrPid()) {
							if (!mtsPacket.adaptationField.discontinuityIndicator) {
								// Get PCR and current nano time
								long pcrValue = mtsPacket.adaptationField.pcr.getValue();
								long pcrTime = System.nanoTime();
								pcrCount++;

								// Compute sleepNanosOrig
								Long sleepNanosOrig = null;
								if (firstPcrValue == null || firstPcrTime == null) {
									firstPcrValue = pcrValue;
									firstPcrTime = pcrTime;
									//firstPcrPacketCount = pcrPidPacketCount;
								} else if (pcrValue > firstPcrValue) {
									sleepNanosOrig = ((pcrValue - firstPcrValue) / 27 * 1000) - (pcrTime - firstPcrTime);
								}

								// Compute sleepNanosPrevious
								Long sleepNanosPrevious = null;
								if (lastPcrValue != null && lastPcrTime != null) {
									if (pcrValue <= lastPcrValue) {
										System.err.println("PCR discontinuity !");
										resetState = true;
									} else {
										sleepNanosPrevious = ((pcrValue - lastPcrValue) / 27 * 1000) - (pcrTime - lastPcrTime);
									}
								}
//								System.out.println("pcrValue=" + pcrValue + ", lastPcrValue=" + lastPcrValue + ", sleepNanosPrevious=" + sleepNanosPrevious + ", sleepNanosOrig=" + sleepNanosOrig);

								// Set sleep time based on PCR if possible
								if (sleepNanosPrevious != null) {
									// Safety : We should never have to wait more than 100ms
									if (sleepNanosPrevious > 100000000) {
										System.err.println("PCR sleep ignored, too high !");
										resetState = true;
									} else {
										sleepNanos = sleepNanosPrevious;
//										averageSleep = sleepNanosPrevious / (pcrPidPacketCount - lastPcrPacketCount - 1);
									}
								}

								// Set lastPcrValue/lastPcrTime
								lastPcrValue = pcrValue;
								lastPcrTime = pcrTime + sleepNanos;
								//lastPcrPacketCount = pcrPidPacketCount;
							} else {
								System.out.println("Skipped PCR - Discontinuity indicator");
							}
						} else {
							System.out.println("Skipped PCR - PID does not match");
						}
					}
				}

				// Sleep if needed
				if (sleepNanos > 0) {
					System.out.println("Sleeping " + sleepNanos / 1000000 + " millis, " + sleepNanos % 1000000 + " nanos");
					Thread.sleep(sleepNanos / 1000000, (int) (sleepNanos % 1000000));
				}

				// Stream packet
				System.out.println("Streaming packet #" + packetCount + ", PID=" + mtsPacket.pid + ", pcrCount=" + pcrCount + ", continuityCounter=" + mtsPacket.continuityCounter);

				sink.send(mtsPacket);

				packetCount++;
			}
			System.out.println("Sent " + packetCount + " MPEG-TS packets");

		}

	public static StreamerBuilder builder() {
		return new StreamerBuilder();
	}

	public static class StreamerBuilder {
		private MTSSink sink;
		private MTSSource source;

		public StreamerBuilder setSink(MTSSink sink) {
			this.sink = sink;
			return this;
		}

		public StreamerBuilder setSource(MTSSource source) {
			this.source = source;
			return this;
		}

		public Streamer build() {
			Preconditions.checkNotNull(sink);
			Preconditions.checkNotNull(source);
			return new Streamer(source, sink);
		}
	}
}