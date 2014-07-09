package org.taktik.mpegts;

import java.io.File;

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
		ResettableMTSSource media123 = MTSSources.from(new File("/Users/abaudoux/Downloads/Media-123.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource media132= MTSSources.from(new File("/Users/abaudoux/Downloads/Media-132.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource media132_2= MTSSources.from(new File("/Users/abaudoux/Downloads/Media-132.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource media133= MTSSources.from(new File("/Users/abaudoux/Downloads/Media-133.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource media133_2 = MTSSources.from(new File("/Users/abaudoux/Downloads/Media-133.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource appleIntro= MTSSources.from(new File("/Users/abaudoux/Downloads/Apple WWDC 2013 Keynote Intro Video.ffmpeg.ts"));

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
				.setSource(MTSSources.loop(media123))
				.setSink(transport)
				.build();

		streamer.stream();

	}
}
