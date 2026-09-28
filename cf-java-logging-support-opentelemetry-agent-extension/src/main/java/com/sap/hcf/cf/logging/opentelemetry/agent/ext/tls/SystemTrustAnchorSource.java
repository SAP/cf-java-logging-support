package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Supplies the X.509 trust anchors known to the JVM's default trust store
 * (typically {@code $JAVA_HOME/lib/security/cacerts}).
 *
 * <p>This is the same set the platform uses to validate ordinary HTTPS connections,
 * so any endpoint whose server certificate chains to a public root is trusted without
 * additional configuration.</p>
 *
 * <p>The stream aggregates the accepted issuers of <em>every</em>
 * {@link X509TrustManager} returned by the default {@link TrustManagerFactory}, so a
 * runtime with multiple configured trust managers contributes all of them.</p>
 */
public class SystemTrustAnchorSource implements X509CertificateSource {

    private static final Logger LOG = Logger.getLogger(SystemTrustAnchorSource.class.getName());

    @Override
    public Stream<X509Certificate> stream() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            return Arrays.stream(tmf.getTrustManagers())
                         .filter(X509TrustManager.class::isInstance)
                         .map(X509TrustManager.class::cast)
                         .flatMap(tm -> Arrays.stream(tm.getAcceptedIssuers()));
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to enumerate JVM default trust anchors; system trust anchors will be omitted.");
            return Stream.empty();
        }
    }
}
