package org.taktik.mpegts;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sinks.UDPTransport;
import org.taktik.mpegts.sources.MTSSource;
import org.taktik.mpegts.sources.MTSSources;
import org.taktik.mpegts.sources.MultiMTSSource;
import org.taktik.mpegts.sources.ResettableMTSSource;

public class StreamerTest {
	public static void main(String[] args) throws Exception {

		// Set up mts sink
		MTSSink transport = UDPTransport.builder()
				//.setAddress("239.222.1.1")
				.setAddress("127.0.0.1")
				.setPort(1234)
				.setSoTimeout(5000)
				.setTtl(1)
				.build();

		// Set up packet source
		ResettableMTSSource media123 = MTSSources.from(new File("/Users/abaudoux/Downloads/Media-123.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource media132 = MTSSources.from(new File("/Users/abaudoux/Downloads/Media-132.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource media133 = MTSSources.from(new File("/Users/abaudoux/Downloads/Media-133.ffmpeg.ts"));

		// Set up packet source
		ResettableMTSSource appleIntro = MTSSources.from(new File("/Users/abaudoux/Downloads/Apple WWDC 2013 Keynote Intro Video.ffmpeg.ts"));

		ResettableMTSSource ts1 = MTSSources.from(new File("/Users/abaudoux/Downloads/ts1.ts"));
		ResettableMTSSource ts2 = MTSSources.from(new File("/Users/abaudoux/Downloads/ts2.ts"));
		ResettableMTSSource ts3 = MTSSources.from(new File("/Users/abaudoux/Downloads/ts3.ts"));
		ResettableMTSSource ts4 = MTSSources.from(new File("/Users/abaudoux/Downloads/ts4.ts"));
		ResettableMTSSource sandiego = MTSSources.from(new File("/Users/abaudoux/Downloads/San_Diego_Clip.ts"));

		// media132, media133 --> ok
		// media133, media132 --> ok
		// media123, media132 --> ko

		FileChannel fc = FileChannel.open(new File("/tmp/out1.ts").toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));

		// Build source
		MTSSource source = MultiMTSSource.builder()
				.setSources(
						//media132
						ts1, ts2, ts3, ts4
				)
				.setFixContinuity(true)
				//.loop()
				.build();

		// build streamer
		Streamer streamer = Streamer.builder()
				.setSource(source)
				//.setSink(ByteChannelSink.builder().setByteChannel(fc).build())
				.setSink(transport)
				.build();

		// Start streaming
		streamer.stream();

		fc.close();
	}
}
