package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.pivotal.cfenv.core.CfCredentials;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudLoggingCredentialsTest {

    private static final String VALID_CLIENT_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "Base-64-Encoded Certificate\n" +
            "-----END CERTIFICATE-----\n";

    private static final String VALID_CLIENT_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "Base-64-Encoded Private Key\n" +
            "-----END PRIVATE KEY-----\n";

    private static final String VALID_SERVER_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "Base-64-Encoded Server Certificate\n" +
            "-----END CERTIFICATE-----\n";

    private static final CloudLoggingCredentials.Parser PARSER = CloudLoggingCredentials.parser();

    @NotNull
    private static Map<String, Object> getValidCredData() {
        Map<String, Object> credData = new HashMap<>();
        credData.put("ingest-otlp-endpoint", "test-endpoint");
        credData.put("ingest-otlp-cert", VALID_CLIENT_CERT);
        credData.put("ingest-otlp-key", VALID_CLIENT_KEY);
        credData.put("server-ca", VALID_SERVER_CERT);
        return credData;
    }

    @Test
    public void validCredentials() {
        Map<String, Object> credData = getValidCredData();
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertTrue("Credentials should be valid", credentials.validate());
    }

    @Test
    public void missingEndpoint() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("ingest-otlp-endpoint");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void missingClientKey() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("ingest-otlp-key");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void missingClientCert() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("ingest-otlp-cert");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void missingServerCert() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("server-ca");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void parsesCorrectly() {
        Map<String, Object> credData = getValidCredData();
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.getEndpoint(), equalTo("https://test-endpoint"));
        assertThat(new String(credentials.getClientCert(), StandardCharsets.UTF_8), equalTo("-----BEGIN CERTIFICATE-----\nBase-64-Encoded Certificate\n-----END CERTIFICATE-----"));
        assertThat(new String(credentials.getClientKey(), StandardCharsets.UTF_8), equalTo("-----BEGIN PRIVATE KEY-----\nBase-64-Encoded Private Key\n-----END PRIVATE KEY-----"));
        assertThat(new String(credentials.getServerCert(), StandardCharsets.UTF_8), equalTo("-----BEGIN CERTIFICATE-----\nBase-64-Encoded Server Certificate\n-----END CERTIFICATE-----"));
    }

}
