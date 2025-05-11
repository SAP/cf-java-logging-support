package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudLoggingCredentialsTest {

    private static final String VALID_CLIENT_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final String VALID_CLIENT_KEY =
            "-----BEGIN PRIVATE KEY-----\n" + "Base-64-Encoded Private Key\n" + "-----END PRIVATE KEY-----\n";

    private static final String VALID_SERVER_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Server Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final CloudLoggingCredentials.Parser PARSER = CloudLoggingCredentials.parser();

    @Test
    public void validCredentials() {
        CloudFoundryCredentials.Builder builder =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-cert", VALID_CLIENT_CERT)
                                       .add("ingest-otlp-key", VALID_CLIENT_KEY).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertTrue("Credentials should be valid", credentials.validate());
    }

    @Test
    public void missingEndpoint() {
        CloudFoundryCredentials.Builder builder =
                CloudFoundryCredentials.builder().add("ingest-otlp-cert", VALID_CLIENT_CERT)
                                       .add("ingest-otlp-key", VALID_CLIENT_KEY).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void missingClientKey() {
        CloudFoundryCredentials.Builder builder =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-cert", VALID_CLIENT_CERT).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void missingClientCert() {
        CloudFoundryCredentials.Builder builder =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", VALID_CLIENT_KEY).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void missingServerCert() {
        CloudFoundryCredentials.Builder builder =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-cert", VALID_CLIENT_CERT)
                                       .add("ingest-otlp-key", VALID_CLIENT_KEY);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertFalse("Credentials should be invalid", credentials.validate());
    }

    @Test
    public void parsesCorrectly() {
        CloudFoundryCredentials.Builder builder =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-cert", VALID_CLIENT_CERT)
                                       .add("ingest-otlp-key", VALID_CLIENT_KEY).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.getEndpoint(), equalTo("https://test-endpoint"));
        assertThat(new String(credentials.getClientCert(), StandardCharsets.UTF_8),
                   equalTo("-----BEGIN CERTIFICATE-----\nBase-64-Encoded Certificate\n-----END CERTIFICATE-----"));
        assertThat(new String(credentials.getClientKey(), StandardCharsets.UTF_8),
                   equalTo("-----BEGIN PRIVATE KEY-----\nBase-64-Encoded Private Key\n-----END PRIVATE KEY-----"));
        assertThat(new String(credentials.getServerCert(), StandardCharsets.UTF_8),
                   equalTo("-----BEGIN CERTIFICATE-----\nBase-64-Encoded Server Certificate\n-----END CERTIFICATE-----"));
    }

}
