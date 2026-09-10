package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static org.assertj.core.api.Assertions.assertThat;

class AppendedTrustCertificatesTest {

    @Test
    void mergedPemContainsBothSystemAnchorsAndServerCert() throws Exception {
        X509Certificate[] systemAnchors = getSystemAnchors();
        assertThat(systemAnchors).as("JVM default trust store must expose at least one anchor").isNotEmpty();

        // Use the first system anchor as our stand-in "server-ca" so we don't have to fabricate one.
        // The merged output should still contain (systemAnchors.length + 1) BEGIN CERTIFICATE markers
        // because the cert appears once via the system-anchor loop and once as the appended trailer.
        byte[] serverCert = toPem(systemAnchors[0]).getBytes(StandardCharsets.US_ASCII);
        byte[] merged = AppendedTrustCertificates.mergedWithSystemDefaults(serverCert);

        String mergedText = new String(merged, StandardCharsets.US_ASCII);
        int markers = countOccurrences(mergedText, "-----BEGIN CERTIFICATE-----");
        assertThat(markers).isEqualTo(systemAnchors.length + 1);

        // The appended cert bytes must be preserved verbatim at the end of the output.
        assertThat(mergedText).endsWith(new String(serverCert, StandardCharsets.US_ASCII));
    }

    @Test
    void resultStartsWithPemHeader() {
        byte[] merged = AppendedTrustCertificates.mergedWithSystemDefaults(new byte[0]);
        String s = new String(merged, StandardCharsets.US_ASCII);
        assertThat(s).startsWith("-----BEGIN CERTIFICATE-----");
    }

    private static X509Certificate[] getSystemAnchors() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return ((X509TrustManager) tm).getAcceptedIssuers();
            }
        }
        return new X509Certificate[0];
    }

    private static String toPem(X509Certificate cert) throws Exception {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                               .encodeToString(cert.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + encoded + "\n-----END CERTIFICATE-----\n";
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
