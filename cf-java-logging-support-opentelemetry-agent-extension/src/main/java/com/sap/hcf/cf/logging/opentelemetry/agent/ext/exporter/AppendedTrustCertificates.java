package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Merges a service-binding-supplied server CA with the JVM's default trust store
 * so that both are honored by the OTLP exporter.
 *
 * <p>SAP Cloud Logging's ingest endpoint chains to a public root (currently Let's
 * Encrypt ISRG Root X1), which the JVM already trusts by default. Passing the raw
 * {@code server-ca} value from the binding to
 * {@link io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder#setTrustedCertificates(byte[])}
 * would replace the JVM trust store with that single anchor, which is brittle
 * against future root rotations. This helper concatenates the JVM's system trust
 * anchors with the supplied server CA, so the exporter trusts both.</p>
 */
final class AppendedTrustCertificates {

    private static final Logger LOG = Logger.getLogger(AppendedTrustCertificates.class.getName());
    private static final String PEM_LINE_SEP = "\n";
    private static final int PEM_LINE_LENGTH = 64;

    private AppendedTrustCertificates() {
    }

    /**
     * Returns a PEM-encoded byte array containing every trust anchor from the JVM's
     * default trust manager, followed by the supplied {@code serverCert} bytes
     * verbatim. If the JVM default trust anchors cannot be enumerated, the
     * {@code serverCert} bytes are returned unchanged.
     *
     * @param serverCert the PEM-encoded server CA from the service binding; must not be {@code null}
     * @return a PEM-encoded byte array suitable for
     *         {@code OtlpGrpc*ExporterBuilder.setTrustedCertificates(byte[])}
     */
    static byte[] mergedWithSystemDefaults(byte[] serverCert) {
        StringBuilder pem = new StringBuilder();
        try {
            for (X509Certificate cert : getSystemTrustAnchors()) {
                appendPem(pem, cert);
            }
        } catch (GeneralSecurityException | IOException e) {
            LOG.warning("Failed to enumerate JVM default trust anchors, falling back to server-ca only: "
                    + e.getMessage());
            return serverCert;
        }
        byte[] systemPem = pem.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] merged = new byte[systemPem.length + serverCert.length];
        System.arraycopy(systemPem, 0, merged, 0, systemPem.length);
        System.arraycopy(serverCert, 0, merged, systemPem.length, serverCert.length);
        return merged;
    }

    private static X509Certificate[] getSystemTrustAnchors() throws GeneralSecurityException, IOException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return ((X509TrustManager) tm).getAcceptedIssuers();
            }
        }
        return new X509Certificate[0];
    }

    private static void appendPem(StringBuilder pem, X509Certificate cert) throws GeneralSecurityException {
        String encoded = Base64.getMimeEncoder(PEM_LINE_LENGTH, PEM_LINE_SEP.getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                               .encodeToString(cert.getEncoded());
        pem.append("-----BEGIN CERTIFICATE-----").append(PEM_LINE_SEP);
        pem.append(encoded).append(PEM_LINE_SEP);
        pem.append("-----END CERTIFICATE-----").append(PEM_LINE_SEP);
    }
}
