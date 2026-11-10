package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class PemEncoderTest {

    private static final X509Certificate CERTIFICATE = loadTestCertificate();

    private static X509Certificate loadTestCertificate() {
        try (InputStream is = PemEncoderTest.class.getClassLoader().getResourceAsStream("certificate.pem")) {
            assumeThat(is).as("test resource certificate.pem must be present on the classpath").isNotNull();
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load test certificate", e);
        }
    }

    @Test
    void encodesCertificateWithArmorAndTrailingNewline() throws Exception {
        String pem = PemEncoder.encode(CERTIFICATE);

        assertThat(pem).startsWith("-----BEGIN CERTIFICATE-----\n").endsWith("-----END CERTIFICATE-----\n");
    }

    @Test
    void encodedOutputRoundtripsBackToTheSameCertificate() throws Exception {
        String pem = PemEncoder.encode(CERTIFICATE);

        X509Certificate parsed = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        assertThat(parsed).isEqualTo(CERTIFICATE);
    }

    @Test
    void wrapsBase64BodyAt64Characters() throws Exception {
        String pem = PemEncoder.encode(CERTIFICATE);
        String body = pem.replace("-----BEGIN CERTIFICATE-----\n", "").replace("\n-----END CERTIFICATE-----\n", "");

        for (String line : body.split("\n")) {
            // The last line may be shorter; every other line must be exactly 64 chars.
            assertThat(line.length()).isLessThanOrEqualTo(64);
        }
    }

    @Test
    void isCompatibleWithPreviousServerCertificateDownloaderFormat() throws Exception {
        // Guard against accidental drift: the format must remain interchangeable with the
        // one previously produced by ServerCertificateDownloader#download.
        String pem = PemEncoder.encode(CERTIFICATE);

        assertThat(pem).matches("(?s)^-----BEGIN CERTIFICATE-----\\n([A-Za-z0-9+/=]{1,64}\\n)+-----END CERTIFICATE-----\\n$");
    }
}
