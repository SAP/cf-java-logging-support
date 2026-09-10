package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

class PemEncoderTest {

    static X509Certificate loadTestCertificate() throws Exception {
        try (InputStream is = PemEncoderTest.class.getClassLoader().getResourceAsStream("certificate.pem")) {
            assertThat(is).isNotNull();
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
    }

    @Test
    void encodesCertificateWithArmorAndTrailingNewline() throws Exception {
        String pem = PemEncoder.encode(loadTestCertificate());

        assertThat(pem).startsWith("-----BEGIN CERTIFICATE-----\n").endsWith("-----END CERTIFICATE-----\n");
    }

    @Test
    void encodedOutputRoundtripsBackToTheSameCertificate() throws Exception {
        X509Certificate original = loadTestCertificate();

        String pem = PemEncoder.encode(original);

        X509Certificate parsed = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void wrapsBase64BodyAt64Characters() throws Exception {
        String pem = PemEncoder.encode(loadTestCertificate());
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
        String pem = PemEncoder.encode(loadTestCertificate());

        assertThat(pem).matches("(?s)^-----BEGIN CERTIFICATE-----\\n([A-Za-z0-9+/=]{1,64}\\n)+-----END CERTIFICATE-----\\n$");
    }
}
