mpegts-streamer
===============

## Introduction
A simple pure java MPEG-TS streamer with a fluent API. 

Example:

```java
// Set up packet source
MTSSource movie = MTSSources.from(new File("/Users/abaudoux/movie.ts"));

// Set up packet sink
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

Simple sources are built from one ts stream. MultiSources are built by 


