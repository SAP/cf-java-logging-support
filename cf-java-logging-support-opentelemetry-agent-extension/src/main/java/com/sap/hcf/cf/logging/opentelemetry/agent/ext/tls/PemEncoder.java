package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Encodes X.509 certificates as PEM strings.
 *
 * <p>Line separator is {@code "\n"} and Base64 body is wrapped at 64 characters,
 * matching the format expected by OpenTelemetry's {@code setTrustedCertificates(byte[])}.</p>
 */
final class PemEncoder {

    private static final String LINE_SEPARATOR = "\n";
    private static final Base64.Encoder BASE64_ENCODER =
            Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes(StandardCharsets.UTF_8));

    private PemEncoder() {
    }

    /**
     * Returns the PEM encoding of the given certificate, including the
     * {@code BEGIN CERTIFICATE} / {@code END CERTIFICATE} armor and a trailing newline.
     */
    static String encode(X509Certificate certificate) throws CertificateEncodingException {
        return "-----BEGIN CERTIFICATE-----" + LINE_SEPARATOR
                + BASE64_ENCODER.encodeToString(certificate.getEncoded()) + LINE_SEPARATOR
                + "-----END CERTIFICATE-----" + LINE_SEPARATOR;
    }
}
