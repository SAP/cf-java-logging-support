package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaasBindingPropertiesSupplierTest {

    @Mock
    private CaasServiceProvider serviceProvider;

    @Mock
    private PemFileCreator pemFileCreator;

    @Mock
    private ServerCertificateDownloader serverCertificateDownloader;

    @Mock
    private CloudFoundryServiceInstance serviceInstance;

    @Mock
    private CloudFoundryCredentials credentials;

    @Mock
    private File serverCertFile;

    @Mock
    private File clientCertFile;

    @Mock
    private File clientKeyFile;

    private CaasBindingPropertiesSupplier supplier;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(serviceProvider.get()).thenReturn(serviceInstance);
        lenient().when(serviceInstance.getCredentials()).thenReturn(credentials);
        lenient().when(serviceInstance.getName()).thenReturn("test-caas-service");
        lenient().when(serverCertFile.getAbsolutePath()).thenReturn("/tmp/server.crt");
        lenient().when(clientCertFile.getAbsolutePath()).thenReturn("/tmp/client.crt");
        lenient().when(clientKeyFile.getAbsolutePath()).thenReturn("/tmp/client.key");

        supplier = new CaasBindingPropertiesSupplier(serviceProvider, pemFileCreator, serverCertificateDownloader);
    }

    @Test
    void shouldReturnEmptyMapWhenNoServiceInstanceFound() {
        when(serviceProvider.get()).thenReturn(null);

        Map<String, String> result = supplier.get();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapWhenServiceHasNoCredentials() {
        when(serviceInstance.getCredentials()).thenReturn(null);

        Map<String, String> result = supplier.get();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapWhenEndpointUrlIsNull() {
        when(credentials.getString("http-url")).thenReturn(null);

        Map<String, String> result = supplier.get();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapWhenEndpointUrlIsBlank() {
        when(credentials.getString("http-url")).thenReturn("   ");

        Map<String, String> result = supplier.get();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnBasicPropertiesWithoutTlsWhenClientCertMissing() {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn(null);
        when(credentials.getString("tls.key")).thenReturn("client-key");

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318")
                          .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                          .containsEntry("otel.exporter.otlp.compression", "gzip")
                          .doesNotContainKey("otel.exporter.otlp.certificate")
                          .doesNotContainKey("otel.exporter.otlp.client.cert")
                          .doesNotContainKey("otel.exporter.otlp.client.key");
    }

    @Test
    void shouldReturnBasicPropertiesWithoutTlsWhenClientKeyMissing() {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn("client-cert");
        when(credentials.getString("tls.key")).thenReturn(null);

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318")
                          .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                          .containsEntry("otel.exporter.otlp.compression", "gzip")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
    }

    @Test
    void shouldReturnBasicPropertiesWhenServerCertDownloadFails() {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn("client-cert");
        when(credentials.getString("tls.key")).thenReturn("client-key");
        when(serverCertificateDownloader.download(anyString())).thenReturn(null);

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318")
                          .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                          .containsEntry("otel.exporter.otlp.compression", "gzip")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
    }

    @Test
    void shouldReturnBasicPropertiesWhenServerCertIsBlank() {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn("client-cert");
        when(credentials.getString("tls.key")).thenReturn("client-key");
        when(serverCertificateDownloader.download(anyString())).thenReturn("   ");

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
    }

    @Test
    void shouldReturnBasicPropertiesWhenPemFileCreationFails() throws IOException {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn("client-cert");
        when(credentials.getString("tls.key")).thenReturn("client-key");
        when(serverCertificateDownloader.download(anyString())).thenReturn("server-cert");
        when(pemFileCreator.writeFile(anyString(), anyString(), anyString())).thenThrow(
                new IOException("Failed to write file"));

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318")
                          .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                          .containsEntry("otel.exporter.otlp.compression", "gzip")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
    }

    @Test
    void shouldReturnFullPropertiesWithTlsConfiguration() throws IOException {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn("client-cert-content");
        when(credentials.getString("tls.key")).thenReturn("client-key-content");
        when(serverCertificateDownloader.download("https://caas.example.com:4318")).thenReturn("server-cert-content");
        when(pemFileCreator.writeFile(eq("caas-server-cert-"), eq(".crt"), eq("server-cert-content"))).thenReturn(
                serverCertFile);
        when(pemFileCreator.writeFile(eq("caas-client-cert-"), eq(".crt"), eq("client-cert-content"))).thenReturn(
                clientCertFile);
        when(pemFileCreator.writeFile(eq("caas-client-key-"), eq(".key"), eq("client-key-content"))).thenReturn(
                clientKeyFile);

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318")
                          .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                          .containsEntry("otel.exporter.otlp.compression", "gzip")
                          .containsEntry("otel.exporter.otlp.certificate", "/tmp/server.crt")
                          .containsEntry("otel.exporter.otlp.client.cert", "/tmp/client.crt")
                          .containsEntry("otel.exporter.otlp.client.key", "/tmp/client.key");
    }

    @Test
    void shouldReplacePlaceholderInEndpointUrl() {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:<http-receiver-port>");

        Map<String, String> result = supplier.get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://caas.example.com:4318");
    }

    @Test
    void shouldNotDownloadServerCertWhenClientCredentialsMissing() {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn(null);
        when(credentials.getString("tls.key")).thenReturn(null);

        supplier.get();

        verify(serverCertificateDownloader, never()).download(anyString());
    }

    @Test
    void shouldNotCreatePemFilesWhenServerCertDownloadFails() throws IOException {
        when(credentials.getString("http-url")).thenReturn("https://caas.example.com:4318");
        when(credentials.getString("tls.crt")).thenReturn("client-cert");
        when(credentials.getString("tls.key")).thenReturn("client-key");
        when(serverCertificateDownloader.download(anyString())).thenReturn(null);

        supplier.get();

        verify(pemFileCreator, never()).writeFile(anyString(), anyString(), anyString());
    }
}
