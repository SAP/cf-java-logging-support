package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerCertificateDownloader {

    private static final Logger LOG = Logger.getLogger(ServerCertificateDownloader.class.getName());
    private static final byte[] LINE_SEPARATOR = "\n".getBytes(StandardCharsets.UTF_8);
    private static final Base64.Encoder BASE64_ENCODER = Base64.getMimeEncoder(64, LINE_SEPARATOR);

    private final SSLSocketFactory sslSocketFactory;

    public ServerCertificateDownloader() {
        this(createDefaultSSLSocketFactory());
    }

    ServerCertificateDownloader(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    private static SSLSocketFactory createDefaultSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllX509(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to create default SSLSocketFactory");
            return null;
        }
    }

    private static TrustManager[] trustAllX509() {
        return new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } };
    }

    public String download(String endpointUrl) {
        try {
            if (sslSocketFactory == null) {
                LOG.warning(
                        () -> "SSLSocketFactory is not initialized, cannot download server certificate from " + endpointUrl);
                return null;
            }
            URL url = new URL(endpointUrl);
            String host = url.getHost();
            int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();

            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
                socket.startHandshake();
                Certificate[] serverCertificates = socket.getSession().getPeerCertificates();

                if (serverCertificates.length == 0) {
                    LOG.warning(() -> "No server certificates found when connecting to " + endpointUrl);
                    return null;
                }

                X509Certificate x509Cert = (X509Certificate) serverCertificates[0];
                byte[] encoded = x509Cert.getEncoded();
                return "-----BEGIN CERTIFICATE-----\n" //
                        + BASE64_ENCODER.encodeToString(encoded) //
                        + "\n-----END CERTIFICATE-----\n";

            }
        } catch (CertificateEncodingException | IOException e) {
            LOG.log(Level.WARNING, e, () -> "Failed to download server certificate from " + endpointUrl);
            return null;
        }
    }
}
