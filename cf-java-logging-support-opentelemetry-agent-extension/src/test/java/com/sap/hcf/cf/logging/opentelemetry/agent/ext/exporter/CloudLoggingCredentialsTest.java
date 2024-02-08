package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.pivotal.cfenv.core.CfCredentials;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class CloudLoggingCredentialsTest {

    private static final String VALID_CLIENT_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final String VALID_CLIENT_KEY =
            "-----BEGIN PRIVATE KEY-----\n" + "Base-64-Encoded Private Key\n" + "-----END PRIVATE KEY-----\n";

    private static final String VALID_SERVER_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Server Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final CloudLoggingCredentials.Parser PARSER = CloudLoggingCredentials.parser();

    private static Map<String, Object> getValidCredData() {
        return new HashMap<>(Map.ofEntries(entry("ingest-otlp-endpoint", "test-endpoint"),
                                           entry("ingest-otlp-cert", VALID_CLIENT_CERT),
                                           entry("ingest-otlp-key", VALID_CLIENT_KEY),
                                           entry("server-ca", VALID_SERVER_CERT)));
    }

    @Test
    public void validCredentials() {
        Map<String, Object> credData = getValidCredData();
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.validate()).isTrue();
    }

    @Test
    public void missingEndpoint() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("ingest-otlp-endpoint");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    public void missingClientKey() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("ingest-otlp-key");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    public void missingClientCert() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("ingest-otlp-cert");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    public void missingServerCert() {
        Map<String, Object> credData = getValidCredData();
        credData.remove("server-ca");
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.validate()).isFalse();
    }

    @Test
    public void parsesCorrectly() {
        Map<String, Object> credData = getValidCredData();
        CfCredentials cfCredentials = new CfCredentials(credData);
        CloudLoggingCredentials credentials = PARSER.parse(cfCredentials);
        assertThat(credentials.getEndpoint()).isEqualTo("https://test-endpoint");
        assertThat(new String(credentials.getClientCert(), StandardCharsets.UTF_8)).isEqualTo(VALID_CLIENT_CERT.trim());
        assertThat(new String(credentials.getClientKey(), StandardCharsets.UTF_8)).isEqualTo(VALID_CLIENT_KEY.trim());
        assertThat(new String(credentials.getServerCert(), StandardCharsets.UTF_8)).isEqualTo(VALID_SERVER_CERT.trim());
    }

}
