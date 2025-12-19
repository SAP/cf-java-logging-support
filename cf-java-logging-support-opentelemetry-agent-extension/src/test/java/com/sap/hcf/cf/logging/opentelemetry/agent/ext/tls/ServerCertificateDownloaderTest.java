package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerCertificateDownloaderTest {

    private static final byte[] CERT_BYTES = "test-certificate-data".getBytes(StandardCharsets.UTF_8);

    @Mock
    private SSLSocketFactory sslSocketFactory;

    @Mock
    private SSLSocket sslSocket;

    @Mock
    private SSLSession sslSession;

    @Mock
    private X509Certificate certificate;

    private ServerCertificateDownloader downloader;

    @BeforeEach
    void setUp() throws Exception {
        // Common mock setup for happy path scenarios
        lenient().when(certificate.getEncoded()).thenReturn(CERT_BYTES);
        lenient().when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] { certificate });
        lenient().when(sslSocket.getSession()).thenReturn(sslSession);
        lenient().when(sslSocketFactory.createSocket(anyString(), anyInt())).thenReturn(sslSocket);

        downloader = new ServerCertificateDownloader(sslSocketFactory);
    }

    @Test
    void shouldDownloadCertificateSuccessfully() throws Exception {
        String result = downloader.download("https://example.com:443");

        assertThat(result).isNotNull().startsWith("-----BEGIN CERTIFICATE-----\n")
                          .endsWith("-----END CERTIFICATE-----\n").contains("\n");
        verify(sslSocket).startHandshake();
        verify(sslSocket).close();
    }

    @Test
    void shouldUseDefaultPortWhenNotSpecified() throws Exception {
        String result = downloader.download("https://example.com");

        assertThat(result).isNotNull();
        verify(sslSocketFactory).createSocket("example.com", 443);
    }

    @Test
    void shouldReturnNullWhenNoCertificatesFound() throws Exception {
        when(sslSession.getPeerCertificates()).thenReturn(new Certificate[0]);

        String result = downloader.download("https://example.com:443");

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenSSLSocketFactoryIsNull() {
        downloader = new ServerCertificateDownloader(null);

        String result = downloader.download("https://example.com:443");

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullOnIOException() throws Exception {
        when(sslSocketFactory.createSocket(anyString(), anyInt())).thenThrow(new IOException("Connection failed"));

        String result = downloader.download("https://example.com:443");

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullOnCertificateEncodingException() throws Exception {
        when(certificate.getEncoded()).thenThrow(new CertificateEncodingException("Encoding failed"));

        String result = downloader.download("https://example.com:443");

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForInvalidUrl() {
        String result = downloader.download("not-a-valid-url");

        assertThat(result).isNull();
    }

    @Test
    void shouldHandleCustomPort() throws IOException {
        String result = downloader.download("https://example.com:4318");

        assertThat(result).isNotNull();
        verify(sslSocketFactory).createSocket("example.com", 4318);
    }

    @Test
    void shouldEncodeCertificateInBase64WithLineBreaks() throws Exception {
        byte[] largeCertBytes = "A".repeat(100).getBytes();
        when(certificate.getEncoded()).thenReturn(largeCertBytes);

        String result = downloader.download("https://example.com");

        assertThat(result).isNotNull().contains("\n").matches(
                "-----BEGIN CERTIFICATE-----\\n([A-Za-z0-9+/=\\n]{1,64}\\n)+-----END CERTIFICATE-----\\n");
    }
}
