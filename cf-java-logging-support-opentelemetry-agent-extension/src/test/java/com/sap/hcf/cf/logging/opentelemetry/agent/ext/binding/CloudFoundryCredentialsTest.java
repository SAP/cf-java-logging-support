package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFoundryCredentialsTest {

    private static final String VALID_CERT =
            "-----BEGIN CERTIFICATE-----\n" + "Base-64-Encoded Certificate\n" + "-----END CERTIFICATE-----\n";

    private static final String VALID_KEY =
            "-----BEGIN PRIVATE KEY-----\n" + "Base-64-Encoded Private Key\n" + "-----END PRIVATE KEY-----\n";

    @Test
    void providesStrings() {
        CloudFoundryCredentials credentials = CloudFoundryCredentials.builder().add("some-key", "some-value").build();

        assertThat(credentials.getString("some-key")).isEqualTo("some-value");
        assertThat(credentials.getString("other-key")).isNull();
        assertThat(credentials.getPEMBytes("some-key")).isEqualTo("some-value".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void formatsPEMBytes() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("cert", VALID_CERT).add("key", VALID_KEY).build();

        assertThat(new String(credentials.getPEMBytes("cert"), StandardCharsets.UTF_8)).isEqualTo(VALID_CERT.trim());
        assertThat(new String(credentials.getPEMBytes("key"), StandardCharsets.UTF_8)).isEqualTo(VALID_KEY.trim());
    }
}
