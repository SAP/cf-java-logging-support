package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.security.cert.X509Certificate;
import java.util.stream.Stream;

/**
 * Supplies zero or more X.509 certificates that should be added to the OTLP exporter's
 * trust anchors. Implementations may source the certificates from a service binding,
 * a network download, the JVM's default trust store, or any other origin.
 *
 * <p>The returned stream must be finite and may be empty if the source cannot deliver
 * a certificate (e.g. a binding field is missing). Implementations should not throw;
 * they should log and return an empty stream so callers can safely aggregate multiple
 * sources with {@link TrustedCertificatesJoiner}.</p>
 */
@FunctionalInterface
public interface X509CertificateSource {

    /**
     * Returns the certificates supplied by this source. Callers must consume the
     * stream fully; sources are not required to be repeatable.
     */
    Stream<X509Certificate> get();
}
