package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class VcapServiceCredentialsTest {

    private static final ConfigProperties TEST_CONFIG = DefaultConfigProperties.createFromMap(
            Map.of(CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_ENDPOINT.getKey(), "url",
                   CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_CLIENT_KEY.getKey(), "client-key",
                   CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_CLIENT_CERT.getKey(), "client-cert",
                   CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_SERVER_CERT.getKey(), "server-ca",
                   CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.AUTH_TOKEN.getKey(), "auth-token"));

    @Test
    void returnsFieldsFromBuilder() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("url", "test-endpoint-url").add("client-key", "test-client-key")
                                       .add("client-cert", "test-client-cert").add("server-ca", "test-server-ca")
                                       .add("auth-token", "test-auth-token").build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(TEST_CONFIG).apply(credentials);

        assertThat(vcapServiceCredentials.getEndpoint()).isEqualTo("test-endpoint-url");
        assertThat(vcapServiceCredentials.getClientKey()).isEqualTo("test-client-key".getBytes(UTF_8));
        assertThat(vcapServiceCredentials.getClientCert()).isEqualTo("test-client-cert".getBytes(UTF_8));
        assertThat(vcapServiceCredentials.getServerCert()).isEqualTo("test-server-ca".getBytes(UTF_8));
        assertThat(vcapServiceCredentials.getAuthToken()).isEqualTo("test-auth-token");
    }

    @Test
    void nullWithoutEndpointCredentialName() {
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        CloudFoundryCredentials ignored = CloudFoundryCredentials.builder().build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(config).apply(ignored);

        assertThat(vcapServiceCredentials).isNull();
    }

    @Test
    void invalidWithoutEndpointUrl() {
        CloudFoundryCredentials empty = CloudFoundryCredentials.builder().build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(TEST_CONFIG).apply(empty);

        assertThat(vcapServiceCredentials.getEndpoint()).isNull();
        assertThat(vcapServiceCredentials.validate()).isFalse();
    }

    @Test
    void validWithEndpointAndAuthToken() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("url", "test-endpoint-url").add("auth-token", "test-auth-token")
                                       .build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(TEST_CONFIG).apply(credentials);

        assertThat(vcapServiceCredentials.validate()).isTrue();
    }

    @Test
    void validWithEndpointAndTlsSecrets() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("url", "test-endpoint-url").add("client-key", "test-client-key")
                                       .add("client-cert", "test-client-cert").add("server-ca", "test-server-ca")
                                       .build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(TEST_CONFIG).apply(credentials);

        assertThat(vcapServiceCredentials.validate()).isTrue();
    }

    @Test
    void validWithEndpointAndTlsSecretsWithoutServerCa() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("url", "test-endpoint-url").add("client-key", "test-client-key")
                                       .add("client-cert", "test-client-cert").build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(TEST_CONFIG).apply(credentials);

        assertThat(vcapServiceCredentials.validate()).isTrue();
    }

    @Test
    void invalidWithEndpointAndNoAuthTokenOrTlsSecrets() {
        CloudFoundryCredentials credentials = CloudFoundryCredentials.builder().add("url", "test-endpoint-url").build();

        VcapServiceCredentials vcapServiceCredentials = VcapServiceCredentials.parser(TEST_CONFIG).apply(credentials);

        assertThat(vcapServiceCredentials.validate()).isFalse();
    }
}
