package org.taktik.mpegts;

import java.io.File;

import org.taktik.mpegts.sinks.MTSSink;
import org.taktik.mpegts.sinks.UDPTransport;
import org.taktik.mpegts.sources.MTSSource;
import org.taktik.mpegts.sources.MTSSources;
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


		ResettableMTSSource ts1 = MTSSources.from(new File("/Users/abaudoux/Downloads/EBSrecording.mpg"));

		// media132, media133 --> ok
		// media133, media132 --> ok
		// media123, media132 --> ko


		// Build source
		MTSSource source = MTSSources.loop(ts1);

		// build streamer
		Streamer streamer = Streamer.builder()
				.setSource(source)
				//.setSink(ByteChannelSink.builder().setByteChannel(fc).build())
				.setSink(transport)
				.build();

		// Start streaming
		streamer.stream();

	}
}
