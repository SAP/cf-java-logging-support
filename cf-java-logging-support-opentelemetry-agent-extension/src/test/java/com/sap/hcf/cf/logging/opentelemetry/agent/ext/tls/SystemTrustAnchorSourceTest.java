package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

class SystemTrustAnchorSourceTest {

    @Test
    void yieldsTheJvmDefaultTrustAnchors() {
        SystemTrustAnchorSource source = new SystemTrustAnchorSource();

        assertThat(source.stream()).isNotEmpty()
                                   .allSatisfy(cert -> assertThat(cert).isInstanceOf(X509Certificate.class));
    }

    @Test
    void repeatedInvocationsProduceIndependentStreams() {
        SystemTrustAnchorSource source = new SystemTrustAnchorSource();

        long first = source.stream().count();
        long second = source.stream().count();

        assertThat(first).isPositive().isEqualTo(second);
    }
}
