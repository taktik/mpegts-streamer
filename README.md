mpegts-streamer
===============

## Introduction
A simple pure java MPEG-TS streamer with a fluent API. 

Example:

```java
// Set up packet source
MTSSource movie = MTSSources.from(new File("/Users/abaudoux/movie.ts"));

// Set up packet sink. We will send packets directly in UDP
MTSSink transport = UDPTransport.builder()
				.setAddress("127.0.0.1") // Can be a multicast address
				.setPort(1234)
				.setSoTimeout(5000)
				.setTtl(1)
				.build();

// Build streamer
Streamer streamer = Streamer.builder()
				.setSource(source) // We will stream this source
				.setSink(transport) // We will send packets to this sink
				.build();

// Start streaming
streamer.stream();

```


## Concepts

The API uses the concepts of packet sources and packet sinks. 
A packet source is a generic provider of TS packets. The corresponding Java interface is `MTSSource`.

A packet sink is a generic consumer of TS packets. The corresponding Java interface is `MTSSink`. 

See section below for implementations.

The streamer reads packets from a packet source, and sends them to a packet sink with the correct timing.


## Sources

Simple sources are built from one ts stream. MultiSources are built by combining existing sources.

A `ResettableMTSSource` is a `MTSSource` than can reset itself. This can be useful when a source has to be streamed more than once.

### Simple sources

A `MTSSource` can be built from several objects:

```java
// From a File
ResettableMTSSource source = MTSSources.from(new File("/Users/abaudoux/movie.ts"));

// From a ByteChannel
ByteChannel byteChannel = ...;
MTSSource source = MTSSources.from(byteChannel);

// If you have got a SeekableByteChannel, you get a ResettableMTSSource
SeekableByteChannel seekableByteChannel = ...;
ResettableMTSSource source = MTSSources.from(seekableByteChannel);

// Works also for a Guava ByteSource
ByteSource bs = ...;
ResettableMTSSource source = MTSSources.from(bs);

// From an InputStream
InputStream is = ...;
ResettableMTSSource source = MTSSources.from(is);
```

#### Looping

From a `ResettableMTSSource`, a looping source can be built:

```java
ResettableMTSSource source = MTSSources.from(new File("/Users/abaudoux/movie.ts"));
// Loop forever
MTSSource loop = MTSSources.loop(source);

// Loop 5 times
MTSSource loop2 = MTSSources.loop(source, 5);
```

### MultiSource

Multiple simple sources can be combined into a MTSSource using a `MultiMTSSource`. The MultiMTSSource will read each source in order, so the resulting MTSSource will be a concatenation of the original sources. This can be useful for streaming a playlist of individual movies.

A `MultiMTSSource` supports looping, if and only if its composing sources all implement `ResettableMTSSource`

Example:
```java
MTSSource movie1 = ...;
MTSSource movie2 = ...;
MTSSource movie3 = ...;

// No looping
MTSSource playList = MultiMTSSource.builder()
	.setSources(movie1, movie2, movie3)
	.build();
	
// Simpler alternate form
MTSSource playList = MTSSources.fromSources(movie1, movie2, movie3);

// Infinite looping
MTSSource playLoop = MultiMTSSource.builder()
	.setSources(movie1, movie2, movie3)
	.loop()
	.build();
	
// Finite number of loops
MTSSource playLoop = MultiMTSSource.builder()
	.setSources(movie1, movie2, movie3)
	.loops(5)	// 5 loops
	.build();
	
// Infinite looping, continuity fix
MTSSource playLoop = MultiMTSSource.builder()
	.setSources(movie1, movie2, movie3)
	.loop()
	.setFixContinuity(true)
	.build();
	
```





