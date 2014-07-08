package org.taktik.mpegts;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;

public class UDPTransport implements MTSSink {

	private final InetSocketAddress inetSocketAddress;
	private final MulticastSocket multicastSocket;


	private UDPTransport(String address, int port, int ttl, int soTimeout) throws IOException {
		// InetSocketAddress
		inetSocketAddress = new InetSocketAddress(address, port);

		// Create the socket but we don't bind it as we are only going to send data
		// Note that we don't have to join the multicast group if we are only sending data and not receiving
		multicastSocket = new MulticastSocket();
		multicastSocket.setReuseAddress(true);
		multicastSocket.setSoTimeout(soTimeout);
		multicastSocket.setTimeToLive(ttl);

	}

	@Override
	public void send(MTSPacket packet) throws IOException {
		ByteBuffer buffer = packet.packet;
		DatagramPacket datagramPacket = new DatagramPacket(buffer.array(), buffer.capacity(), inetSocketAddress);
		multicastSocket.send(datagramPacket);
	}

	public static UDPTransport.UDPTransportBuilder builder() {
		return new UDPTransportBuilder();
	}

	public static class UDPTransportBuilder {
		private String address;
		private int port;
		private int ttl;
		private int soTimeout;

		public UDPTransportBuilder setAddress(String address) {
			this.address = address;
			return this;
		}

		public UDPTransportBuilder setPort(int port) {
			this.port = port;
			return this;
		}

		public UDPTransportBuilder setTtl(int ttl) {
			this.ttl = ttl;
			return this;
		}

		public UDPTransportBuilder setSoTimeout(int timeout) {
			this.soTimeout = timeout;
			return this;
		}

		public UDPTransport build() throws IOException {
			return new UDPTransport(address, port, ttl, soTimeout);
		}
	}
}
