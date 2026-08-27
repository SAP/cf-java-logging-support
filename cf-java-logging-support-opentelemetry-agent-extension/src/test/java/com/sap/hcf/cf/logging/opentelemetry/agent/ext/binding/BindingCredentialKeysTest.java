package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BindingCredentialKeysTest {

    @Test
    void builderStoresAllConfiguredValues() {
        BindingCredentialKeys keys = BindingCredentialKeys.builder()
                .endpointUrlKey("url")
                .clientCertKey("tls.crt")
                .clientKeyKey("tls.key")
                .serverCaCertKey("tls.ca.crt")
                .tokenKey("token")
                .clientCertFilePrefix("client-cert-")
                .clientKeyFilePrefix("client-key-")
                .serverCaFilePrefix("server-ca-")
                .build();

        assertThat(keys.endpointUrlKey).isEqualTo("url");
        assertThat(keys.clientCertKey).isEqualTo("tls.crt");
        assertThat(keys.clientKeyKey).isEqualTo("tls.key");
        assertThat(keys.serverCaCertKey).isEqualTo("tls.ca.crt");
        assertThat(keys.tokenKey).isEqualTo("token");
        assertThat(keys.clientCertFilePrefix).isEqualTo("client-cert-");
        assertThat(keys.clientKeyFilePrefix).isEqualTo("client-key-");
        assertThat(keys.serverCaFilePrefix).isEqualTo("server-ca-");
    }

    @Test
    void defaultFilePrefixes() {
        BindingCredentialKeys keys = BindingCredentialKeys.builder()
                .endpointUrlKey("url")
                .build();

        assertThat(keys.clientCertFilePrefix).isEqualTo("client-cert-");
        assertThat(keys.clientKeyFilePrefix).isEqualTo("client-key-");
        assertThat(keys.serverCaFilePrefix).isEqualTo("server-ca-");
    }
}
