package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BindingServerCertificateSourceTest {

    private static byte[] validPemBytes() throws Exception {
        try (InputStream is = BindingServerCertificateSourceTest.class.getClassLoader()
                                                                      .getResourceAsStream("certificate.pem")) {
            assertThat(is).isNotNull();
            return is.readAllBytes();
        }
    }

    @Test
    void parsesValidPemIntoASingleCertificate() throws Exception {
        BindingServerCertificateSource source = new BindingServerCertificateSource(validPemBytes());

        assertThat(source.get()).hasSize(1);
    }

    @Test
    void yieldsEmptyStreamForNullInput() {
        BindingServerCertificateSource source = new BindingServerCertificateSource(null);

        assertThat(source.get()).isEmpty();
    }

    @Test
    void yieldsEmptyStreamForEmptyInput() {
        BindingServerCertificateSource source = new BindingServerCertificateSource(new byte[0]);

        assertThat(source.get()).isEmpty();
    }

    @Test
    void yieldsEmptyStreamForMalformedInput() {
        BindingServerCertificateSource source =
                new BindingServerCertificateSource("not a pem".getBytes(StandardCharsets.UTF_8));

        assertThat(source.get()).isEmpty();
    }
}
