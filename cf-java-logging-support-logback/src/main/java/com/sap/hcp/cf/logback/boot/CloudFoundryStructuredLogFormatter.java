package com.sap.hcp.cf.logback.boot;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.encoder.JsonEncoder;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

/**
 * A Logback events formatter for structured logging in SpringBoot
 * configuration, suited
 * for Cloud Foundry applications. This class can be used as a value for
 * {@code logging.structured.format.console}
 * or @{code logging.structured.format.file} in a SpringBoot applications.
 * <p>
 * This is a simple wrapper around the {@code JsonEncoder} created for Logback.
 * This formatter does not accept any configuration parameters and uses the default
 * settings coming with JsonEncoder. The JSON output will contain the same fields 
 * as produced with {@code JsonEncoder}, but you don't have to use logback.xml 
 * (or logback-spring.xml) to use it.
  * <p>
 * Example usage with SpringBoot ({@code application.properties}):
 * 
 * <pre>
 * logging.structured.format.console = com.sap.hcp.cf.logback.boot.CloudFoundryStructuredLogFormatter
 * </pre>
 */
public class CloudFoundryStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

    private final JsonEncoder jsonEncoder;

    public CloudFoundryStructuredLogFormatter() {
        this.jsonEncoder = JsonEncoder.createStarted();
    }

    @Override
    public String format(ILoggingEvent event) {
        return jsonEncoder.getJson(event);
    }

}
