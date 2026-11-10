package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class BindingServerCertificateSourceTest {

    private static byte[] validPemBytes() throws Exception {
        try (InputStream is = BindingServerCertificateSourceTest.class.getClassLoader()
                                                                      .getResourceAsStream("certificate.pem")) {
            assumeThat(is).as("test resource certificate.pem must be present on the classpath").isNotNull();
            return is.readAllBytes();
        }
    }

    @Test
    void parsesValidPemIntoASingleCertificate() throws Exception {
        BindingServerCertificateSource source = new BindingServerCertificateSource(validPemBytes());

        assertThat(source.stream()).hasSize(1);
    }

    @Test
    void yieldsEmptyStreamForNullInput() {
        BindingServerCertificateSource source = new BindingServerCertificateSource(null);

        assertThat(source.stream()).isEmpty();
    }

    @Test
    void yieldsEmptyStreamForEmptyInput() {
        BindingServerCertificateSource source = new BindingServerCertificateSource(new byte[0]);

        assertThat(source.stream()).isEmpty();
    }

    @Test
    void yieldsEmptyStreamForMalformedInput() {
        BindingServerCertificateSource source =
                new BindingServerCertificateSource("not a pem".getBytes(StandardCharsets.UTF_8));

        assertThat(source.stream()).isEmpty();
    }
}
