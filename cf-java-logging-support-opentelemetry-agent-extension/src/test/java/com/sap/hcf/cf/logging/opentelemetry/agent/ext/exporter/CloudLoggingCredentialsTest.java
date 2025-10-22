package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class CloudLoggingCredentialsTest {

    private static final String VALID_CLIENT_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final String VALID_CLIENT_KEY =
            "-----BEGIN PRIVATE KEY-----\n" + "Base-64-Encoded Private Key\n" + "-----END PRIVATE KEY-----\n";

    private static final String VALID_SERVER_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Server Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final CloudLoggingCredentials.Parser PARSER = CloudLoggingCredentials.parser();

    @Test
    void validCredentials() {
        CloudFoundryCredentials.Builder builder =
                builder().add("ingest-otlp-endpoint", "test-endpoint").add("ingest-otlp-cert", VALID_CLIENT_CERT)
                         .add("ingest-otlp-key", VALID_CLIENT_KEY).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.validate()).isTrue();
    }

    @Test
    void missingEndpoint() {
        CloudFoundryCredentials.Builder builder =
                builder().add("ingest-otlp-cert", VALID_CLIENT_CERT).add("ingest-otlp-key", VALID_CLIENT_KEY)
                         .add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    void missingClientKey() {
        CloudFoundryCredentials.Builder builder =
                builder().add("ingest-otlp-endpoint", "test-endpoint").add("ingest-otlp-cert", VALID_CLIENT_CERT)
                         .add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    void missingClientCert() {
        CloudFoundryCredentials.Builder builder =
                builder().add("ingest-otlp-endpoint", "test-endpoint").add("ingest-otlp-key", VALID_CLIENT_KEY)
                         .add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    void missingServerCert() {
        CloudFoundryCredentials.Builder builder =
                builder().add("ingest-otlp-endpoint", "test-endpoint").add("ingest-otlp-cert", VALID_CLIENT_CERT)
                         .add("ingest-otlp-key", VALID_CLIENT_KEY);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    void parsesCorrectly() {
        CloudFoundryCredentials.Builder builder =
                builder().add("ingest-otlp-endpoint", "test-endpoint").add("ingest-otlp-cert", VALID_CLIENT_CERT)
                         .add("ingest-otlp-key", VALID_CLIENT_KEY).add("server-ca", VALID_SERVER_CERT);
        CloudLoggingCredentials credentials = PARSER.parse(builder.build());
        assertThat(credentials.getEndpoint()).isEqualTo("https://test-endpoint");
        assertThat(new String(credentials.getClientCert(), StandardCharsets.UTF_8)).isEqualTo(VALID_CLIENT_CERT.trim());
        assertThat(new String(credentials.getClientKey(), StandardCharsets.UTF_8)).isEqualTo(VALID_CLIENT_KEY.trim());
        assertThat(new String(credentials.getServerCert(), StandardCharsets.UTF_8)).isEqualTo(VALID_SERVER_CERT.trim());
    }

}
