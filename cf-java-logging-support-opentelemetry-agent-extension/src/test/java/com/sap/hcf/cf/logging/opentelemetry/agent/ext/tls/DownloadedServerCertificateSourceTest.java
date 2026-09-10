package com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadedServerCertificateSourceTest {

    private static final String ENDPOINT = "https://example.com:443";

    @Mock
    private ServerCertificateDownloader downloader;

    private static String validPem() throws Exception {
        try (InputStream is = DownloadedServerCertificateSourceTest.class.getClassLoader()
                                                                         .getResourceAsStream("certificate.pem")) {
            assertThat(is).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesDownloadedPemIntoASingleCertificate() throws Exception {
        when(downloader.download(ENDPOINT)).thenReturn(validPem());
        DownloadedServerCertificateSource source = new DownloadedServerCertificateSource(downloader, ENDPOINT);

        assertThat(source.get()).hasSize(1);
    }

    @Test
    void yieldsEmptyStreamWhenDownloaderReturnsNull() {
        when(downloader.download(ENDPOINT)).thenReturn(null);
        DownloadedServerCertificateSource source = new DownloadedServerCertificateSource(downloader, ENDPOINT);

        assertThat(source.get()).isEmpty();
    }

    @Test
    void yieldsEmptyStreamWhenDownloaderReturnsMalformedPem() {
        when(downloader.download(ENDPOINT)).thenReturn("not a pem");
        DownloadedServerCertificateSource source = new DownloadedServerCertificateSource(downloader, ENDPOINT);

        assertThat(source.get()).isEmpty();
    }
}
