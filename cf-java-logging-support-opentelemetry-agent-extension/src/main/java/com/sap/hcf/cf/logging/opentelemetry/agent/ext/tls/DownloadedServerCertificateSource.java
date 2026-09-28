package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Supplies the X.509 leaf certificate returned by {@link ServerCertificateDownloader}
 * for a fixed OTLP endpoint URL.
 *
 * <p>This is the fallback path used when a service binding does not carry a
 * {@code server-ca} field. It connects to the endpoint with TLS (validation disabled)
 * and reads back the leaf certificate the server presents.</p>
 */
public class DownloadedServerCertificateSource implements X509CertificateSource {

    private static final Logger LOG = Logger.getLogger(DownloadedServerCertificateSource.class.getName());

    private final ServerCertificateDownloader downloader;
    private final String endpointUrl;

    public DownloadedServerCertificateSource(ServerCertificateDownloader downloader, String endpointUrl) {
        this.downloader = downloader;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public Stream<X509Certificate> stream() {
        String pem = downloader.download(endpointUrl);
        if (pem == null || pem.isEmpty()) {
            return Stream.empty();
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory
                    .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
            return Stream.of(certificate);
        } catch (CertificateException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to parse server certificate downloaded from " + endpointUrl
                    + "; it will be omitted from the trust anchors.");
            return Stream.empty();
        }
    }
}
