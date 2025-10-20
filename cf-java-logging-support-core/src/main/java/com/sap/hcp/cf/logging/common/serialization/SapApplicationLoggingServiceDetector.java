package com.sap.hcp.cf.logging.common.serialization;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.sap.hcp.cf.logging.common.helper.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SapApplicationLoggingServiceDetector {

    private static final String VCAP_SERVICES = "VCAP_SERVICES";
    private static final String SAP_APPLICATION_LOGGING_LABEL = "application-logs";
    private static final String OVERRIDE_PROPERTY_NAME =
            Environment.LOG_GENERATE_APPLICATION_LOGGING_CUSTOM_FIELDS.toLowerCase().replace("_", ".");

    // Provide logger after constructor ran to give logging backend time for initialization
    private static Logger logger() {
        return LoggerFactory.getLogger(SapApplicationLoggingServiceDetector.class);
    }

    private boolean boundToSapApplicationLogging;

    public SapApplicationLoggingServiceDetector() {
        this(new Environment());
    }

    SapApplicationLoggingServiceDetector(Environment environment) {
        this(environment.getVariable(Environment.VCAP_SERRVICES));
        String overrideEnvironment =
                environment.getVariable(Environment.LOG_GENERATE_APPLICATION_LOGGING_CUSTOM_FIELDS);
        String overrideProperty = System.getProperty(OVERRIDE_PROPERTY_NAME);
        if (Boolean.parseBoolean(overrideEnvironment) || Boolean.parseBoolean(overrideProperty)) {
            this.boundToSapApplicationLogging = true;
        }
    }

    SapApplicationLoggingServiceDetector(String vcapServicesJson) {
        if (vcapServicesJson == null) {
            logger().debug("No Cloud Foundry environment variable " + VCAP_SERVICES + " found.");
            return;
        }
        try (JsonParser parser = new JsonFactory().createParser(vcapServicesJson)) {
            parser.nextToken();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String label = parser.currentName();
                if (SAP_APPLICATION_LOGGING_LABEL.equals(label)) {
                    this.boundToSapApplicationLogging = true;
                    return;
                }
                parser.skipChildren();
            }
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isBoundToSapApplicationLogging() {
        return boundToSapApplicationLogging;
    }
}
