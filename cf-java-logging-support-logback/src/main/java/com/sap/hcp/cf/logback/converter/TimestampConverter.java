package com.sap.hcp.cf.logback.converter;

import java.time.Instant;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * This is a simple {@link ClassicConverter} implementation that prints the
 * timestamp as a long in nano second resolution. Note: nano second precision is
 * only available from Java 9 or newer. Java 8 will only have milli seconds.
 */
public class TimestampConverter extends ClassicConverter {

    public static final String WORD = "tstamp";

    @Override
    public String convert(ILoggingEvent event) {
        Instant now = Instant.now();
        long timestamp = now.getEpochSecond() * 1_000_000_000L + now.getNano();
        return String.valueOf(timestamp);
    }

    @Override
    public void start() {
        super.start();
    }
}
