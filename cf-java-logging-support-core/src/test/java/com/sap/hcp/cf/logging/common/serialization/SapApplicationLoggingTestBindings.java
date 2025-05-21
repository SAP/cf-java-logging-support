package com.sap.hcp.cf.logging.common.serialization;

import com.sap.hcp.cf.logging.common.helper.Environment;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public enum SapApplicationLoggingTestBindings {
    NO_SAP_APPLICATION_LOGGING_BINDING("no-application-logging-binding.json"),
    NO_SAP_APPLICATION_LOGGING_BINDING_BUT_OVERRIDE("no-application-logging-binding.json", true),
    JUST_ONE_SAP_APPLICATION_LOGGING_BINDING("just-one-application-logging-binding.json"),
    MIXED_SAP_APPLICATION_LOGGING_AND_OTHER_BINDING("mixed-no-one-application-logging-binding.json");

    private SapApplicationLoggingServiceDetector detector;

    SapApplicationLoggingTestBindings(String resourcePath) {
        this(resourcePath, false);
    }

    SapApplicationLoggingTestBindings(String resourcePath, boolean assumeBinding) {
        try {
            String json = Files.readString(Paths.get(getClass().getResource(resourcePath).toURI()), UTF_8);
            TestEnvironment environment = new TestEnvironment(assumeBinding, json);
            this.detector = new SapApplicationLoggingServiceDetector(environment);
        } catch (Exception cause) {
            Assertions.fail("Cannot access resource " + resourcePath, cause);
        }

    }

    public SapApplicationLoggingServiceDetector getDetector() {
        return detector;
    }

    private static class TestEnvironment extends Environment {

        private final boolean assumeBinding;
        private final String vcapServices;

        private TestEnvironment(boolean assumeBinding, String vcapServices) {

            this.assumeBinding = assumeBinding;
            this.vcapServices = vcapServices;
        }

        @Override
        public String getVariable(String name) {
            if (Environment.LOG_GENERATE_APPLICATION_LOGGING_CUSTOM_FIELDS.equals(name)) {
                return Boolean.toString(assumeBinding);
            }
            if (Environment.VCAP_SERRVICES.equals(name)) {
                return vcapServices;
            }
            return super.getVariable(name);
        }
    }
}
