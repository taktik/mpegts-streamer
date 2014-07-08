package org.taktik.mpegts;

import java.io.File;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jcodec.common.NIOUtils;
import org.jcodec.containers.mps.MTSUtils;
import org.jcodec.containers.mps.psi.PMTSection;

public class UdpStreamer {
	public static final int MPEGTS_PACKET_SIZE = 188;
	public static final int NUMBER_OF_MPEGTS_PACKET_AT_ONCE = 7;

	public static void main(String[] filePaths) throws Exception {
		// Config
		String address = "127.0.0.1";
		int port = 1234;
		int ttl = 1;

		// InetSocketAddress
		InetSocketAddress inetSocketAddress = new InetSocketAddress(address, port);

		// Create the socket but we don't bind it as we are only going to send data
		// Note that we don't have to join the multicast group if we are only sending data and not receiving
		MulticastSocket multicastSocket = new MulticastSocket();
		multicastSocket.setReuseAddress(true);
		multicastSocket.setSoTimeout(5000);

		// Loop on file paths
		for (int f = 0; f < filePaths.length;) {
			// Get file and inputStream
			String filePath = filePaths[f];
			File file = new File(filePath);
			FileChannel channel = FileChannel.open(file.toPath());

			MTSUtils.PMTExtractor pmtExtractor = new MTSUtils.PMTExtractor();
			pmtExtractor.readTsFile(channel);
			PMTSection pmt = pmtExtractor.getPmt();

			// Fill the buffer with some data
			MpegTsPacket mtsPacket;
			long packetCount = 0;
			long pcrCount = 0;
			long pcrPidPacketCount = 0;
			Long firstPcrValue = null;
			Long firstPcrTime = null;
			Long firstPcrPacketCount = null;
			Long lastPcrValue = null;
			Long lastPcrTime = null;
			Long lastPcrPacketCount = null;
			Long averageSleep = null;
			while (true) {
				// Initialize time to sleep
				long sleepNanos = 0;

				// Get next packet
				ByteBuffer buffer = ByteBuffer.allocate(MPEGTS_PACKET_SIZE);
				if (NIOUtils.read(channel, buffer) != MPEGTS_PACKET_SIZE) {
					break;
				}
				buffer.flip();

				// Parse the packet
				mtsPacket = MpegTsPacket.parsePacket(buffer);
				buffer.rewind();
				if (mtsPacket == null) {
					System.err.println("Cannot parse packet");
					break;
				}

				// Check PID matches PCR PID
				if (mtsPacket.pid == pmt.getPcrPid()) {
					pcrPidPacketCount++;

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
						if (mtsPacket.pid == pmt.getPcrPid()) {
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
									firstPcrPacketCount = pcrPidPacketCount;
								} else if (pcrValue > firstPcrValue) {
									sleepNanosOrig = ((pcrValue - firstPcrValue) / 27 * 1000) - (pcrTime - firstPcrTime);
								}

								// Compute sleepNanosPrevious
								Long sleepNanosPrevious = null;
								if (lastPcrValue != null && lastPcrTime != null) {
									if (pcrValue <= lastPcrValue) {
										System.err.println("PCR discontinuity !");
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
									} else {
										sleepNanos = sleepNanosPrevious;
//										averageSleep = sleepNanosPrevious / (pcrPidPacketCount - lastPcrPacketCount - 1);
									}
								}

								// Set lastPcrValue/lastPcrTime
								lastPcrValue = pcrValue;
								lastPcrTime = pcrTime + sleepNanos;
								lastPcrPacketCount = pcrPidPacketCount;
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
				System.out.println("Streaming packet #" + packetCount + ", PID=" + mtsPacket.pid + ", pcrCount=" + pcrCount + ", pcrPidPacketCount=" + pcrPidPacketCount + ", continuityCounter=" + mtsPacket.continuityCounter);
				DatagramPacket datagramPacket = new DatagramPacket(buffer.array(), buffer.capacity(), inetSocketAddress);
				int previousTtl = multicastSocket.getTimeToLive();
				multicastSocket.setTimeToLive(ttl);
				multicastSocket.send(datagramPacket);
				multicastSocket.setTimeToLive(previousTtl);

				packetCount++;
			}
			System.out.println("Sent " + packetCount + " MPEG-TS packets");

			// Close inputStream
			channel.close();

			// Sleep between files
//			Thread.sleep(2000);

			// Loop on all files
			f++;
			if (f == filePaths.length) {
				f = 0;
			}
		}

		// And when we have finished sending data close the socket
		multicastSocket.close();
	}
}