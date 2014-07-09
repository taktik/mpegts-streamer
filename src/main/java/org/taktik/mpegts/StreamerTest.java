package org.taktik.mpegts;

import java.io.File;
import java.nio.channels.FileChannel;

public class StreamerTest {
	public static void main(String[] args) throws Exception {

		// Set up mts sink
		MTSSink transport = UDPTransport.builder()
				.setAddress("127.0.0.1")
				.setPort(1234)
				.setSoTimeout(5000)
				.setTtl(1)
				.build();

		// Set up packet source
		MTSSource media123 = ByteChannelMTSSource.builder()
				.setByteChannel(FileChannel.open(new File("/Users/abaudoux/Downloads/Media-123.ffmpeg.ts").toPath()))
				.build();


		// Set up packet source
		MTSSource media132= ByteChannelMTSSource.builder()
				.setByteChannel(FileChannel.open(new File("/Users/abaudoux/Downloads/Media-132.ffmpeg.ts").toPath()))
				.build();

		// Set up packet source
		MTSSource media132_2= ByteChannelMTSSource.builder()
				.setByteChannel(FileChannel.open(new File("/Users/abaudoux/Downloads/Media-132.ffmpeg.ts").toPath()))
				.build();

		// Set up packet source
		MTSSource media133= ByteChannelMTSSource.builder()
				.setByteChannel(FileChannel.open(new File("/Users/abaudoux/Downloads/Media-133.ffmpeg.ts").toPath()))
				.build();

		// Set up packet source
		MTSSource media133_2 = ByteChannelMTSSource.builder()
				.setByteChannel(FileChannel.open(new File("/Users/abaudoux/Downloads/Media-133.ffmpeg.ts").toPath()))
				.build();

		// Set up packet source
		MTSSource appleIntro= ByteChannelMTSSource.builder()
				.setByteChannel(FileChannel.open(new File("/Users/abaudoux/Downloads/Apple WWDC 2013 Keynote Intro Video.ffmpeg.ts").toPath()))
				.build();

		// media132, media133 --> ok
		// media133, media132 --> ok
		// media123, media132 --> ko

		// Build source
		MTSSource source = MultiMTSSource.builder()
				.setSources(media123, media132)
				.setFixContinuity(false)
				.build();

		// build streamer
		Streamer streamer = Streamer.builder()
				.setSource(source)
				.setSink(transport)
				.build();

		streamer.stream();

	}
}
