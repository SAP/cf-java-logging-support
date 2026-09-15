package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Supplies the X.509 certificate carried by a Cloud Foundry service binding's
 * {@code server-ca} field (or an equivalent field name for other bindings).
 *
 * <p>The input is expected to be the raw PEM bytes as they arrive in the binding.
 * A {@code null} or empty input yields an empty stream, so the source can be used
 * unconditionally with {@link TrustedCertificatesJoiner}.</p>
 */
public class BindingServerCertificateSource implements X509CertificateSource {

    private static final Logger LOG = Logger.getLogger(BindingServerCertificateSource.class.getName());

    private final byte[] pemBytes;

    public BindingServerCertificateSource(byte[] pemBytes) {
        this.pemBytes = pemBytes;
    }

    @Override
    public Stream<X509Certificate> stream() {
        if (pemBytes == null || pemBytes.length == 0) {
            return Stream.empty();
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(pemBytes));
            return Stream.of(certificate);
        } catch (CertificateException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to parse server-ca from service binding; it will be omitted from the trust anchors.");
            return Stream.empty();
        }
    }
}
