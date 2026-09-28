package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concatenates the PEM encodings of the certificates from one or more
 * {@link X509CertificateSource} instances into a single byte array suitable for
 * {@code OtlpGrpc*ExporterBuilder.setTrustedCertificates(byte[])}.
 *
 * <p>Sources are consumed in the order given; certificates that fail to encode
 * are logged and skipped so a single bad certificate does not break the exporter.</p>
 */
public final class TrustedCertificatesJoiner {

    private static final Logger LOG = Logger.getLogger(TrustedCertificatesJoiner.class.getName());

    private TrustedCertificatesJoiner() {
    }

    /**
     * Encodes every certificate produced by the given sources as PEM and returns the
     * concatenated bytes (UTF-8). Empty sources contribute nothing; the returned array
     * is empty if no source yields a certificate.
     */
    public static byte[] toPemBytes(X509CertificateSource... sources) {
        StringBuilder pem = new StringBuilder();
        for (X509CertificateSource source : sources) {
            source.stream().forEach(cert -> appendPem(pem, cert));
        }
        return pem.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendPem(StringBuilder pem, X509Certificate cert) {
        try {
            pem.append(PemEncoder.encode(cert));
        } catch (CertificateEncodingException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to PEM-encode a trust anchor; it will be omitted.");
        }
    }
}
