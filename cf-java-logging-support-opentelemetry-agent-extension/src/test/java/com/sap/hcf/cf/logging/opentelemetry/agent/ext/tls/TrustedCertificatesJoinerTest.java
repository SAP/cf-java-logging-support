package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class TrustedCertificatesJoinerTest {

    private static byte[] validPemBytes() throws Exception {
        try (InputStream is = TrustedCertificatesJoinerTest.class.getClassLoader()
                                                                 .getResourceAsStream("certificate.pem")) {
            assumeThat(is).as("test resource certificate.pem must be present on the classpath").isNotNull();
            return is.readAllBytes();
        }
    }

    @Test
    void returnsEmptyBytesWhenNoSourcesProvided() {
        assertThat(TrustedCertificatesJoiner.toPemBytes()).isEmpty();
    }

    @Test
    void returnsEmptyBytesWhenAllSourcesAreEmpty() {
        byte[] result = TrustedCertificatesJoiner.toPemBytes(new BindingServerCertificateSource(null),
                                                             new BindingServerCertificateSource(new byte[0]));

        assertThat(result).isEmpty();
    }

    @Test
    void concatenatesCertificatesFromMultipleSourcesInOrder() throws Exception {
        byte[] result = TrustedCertificatesJoiner.toPemBytes(new SystemTrustAnchorSource(),
                                                             new BindingServerCertificateSource(validPemBytes()));

        String pem = new String(result, StandardCharsets.UTF_8);
        long beginMarkers = pem.lines().filter(l -> l.equals("-----BEGIN CERTIFICATE-----")).count();
        long endMarkers = pem.lines().filter(l -> l.equals("-----END CERTIFICATE-----")).count();
        assertThat(beginMarkers).isEqualTo(endMarkers).isGreaterThan(1);

        // The binding source is passed last, so its PEM must appear at the end of the joined output.
        byte[] bindingOnly = TrustedCertificatesJoiner.toPemBytes(new BindingServerCertificateSource(validPemBytes()));
        assertThat(pem).endsWith(new String(bindingOnly, StandardCharsets.UTF_8));
    }
}
