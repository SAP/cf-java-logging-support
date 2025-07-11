package com.sap.hcp.cf.logback.boot;

import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Test;

public class CloudFoundryStructuredLogFormatterTest {
    
    CloudFoundryStructuredLogFormatter underTest = new CloudFoundryStructuredLogFormatter();
    
    @Test
    public void testFormat() throws Exception {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.DEBUG);
        event.setLoggerName("my-logger");
        event.setMessage("This is some very important log message");
        event.setThreadName("thread-1");

        String result = underTest.format(event);

        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}\n"));
        assertTrue(result.contains("\"level\":\"DEBUG\""));
        assertTrue(result.contains("\"logger\":\"my-logger\""));
        assertTrue(result.contains("\"msg\":\"This is some very important log message\""));
        assertTrue(result.contains("\"thread\":\"thread-1\""));

    }

}
