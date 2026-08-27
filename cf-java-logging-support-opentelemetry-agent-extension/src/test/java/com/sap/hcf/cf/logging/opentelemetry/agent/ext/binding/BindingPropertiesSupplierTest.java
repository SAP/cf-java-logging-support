package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BindingPropertiesSupplierTest {

    private static final BindingCredentialKeys KEYS = BindingCredentialKeys.builder()
            .endpointUrlKey("endpoint")
            .clientCertKey("client-cert")
            .clientKeyKey("client-key")
            .serverCaCertKey("ca-cert")
            .tokenKey("auth-token")
            .clientCertFilePrefix("gen-client-cert-")
            .clientKeyFilePrefix("gen-client-key-")
            .serverCaFilePrefix("gen-server-ca-")
            .build();

    @Mock private PemFileCreator pemFileCreator;
    @Mock private CloudFoundryServiceInstance instance;
    @Mock private CloudFoundryCredentials creds;
    @Mock private File certFile;
    @Mock private File keyFile;
    @Mock private File caFile;

    private BindingPropertiesSupplier newSupplier(Optional<CloudFoundryServiceInstance> serviceInstance) {
        return BindingPropertiesSupplier.builder("my-binding", serviceInstance, pemFileCreator, KEYS).build();
    }

    private BindingPropertiesSupplier newSupplierWith(Optional<CloudFoundryServiceInstance> serviceInstance,
                                                      String scheme,
                                                      Function<String, String> serverCaCertProvider,
                                                      String protocol,
                                                      String compression) {
        BindingPropertiesSupplier.Builder b = BindingPropertiesSupplier
                .builder("my-binding", serviceInstance, pemFileCreator, KEYS);
        if (scheme != null) b.scheme(scheme);
        if (serverCaCertProvider != null) b.serverCaCertProvider(serverCaCertProvider);
        if (protocol != null) b.protocol(protocol);
        if (compression != null) b.compression(compression);
        return b.build();
    }

    @Test
    void returnsEmptyWhenBindingNotFound() {
        Map<String, String> result = newSupplier(Optional.empty()).get();

        assertThat(result).isEmpty();
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void returnsEmptyWhenCredentialsMissing() {
        when(instance.getCredentials()).thenReturn(null);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result).isEmpty();
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void returnsEmptyWhenEndpointKeyBlank() {
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("   ");

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result).isEmpty();
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void returnsBasicPropsWithoutCertWhenServerCaMissing() {
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(null);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://gen.example.com:4318")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void setsServerCaOnlyWhenClientCredentialsAbsent() throws Exception {
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.endpoint", "https://gen.example.com:4318")
                .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                .containsEntry("otel.exporter.otlp.compression", "gzip")
                .containsEntry("otel.exporter.otlp.certificate", "/tmp/ca.crt")
                .doesNotContainKey("otel.exporter.otlp.client.certificate")
                .doesNotContainKey("otel.exporter.otlp.client.key")
                .doesNotContainKey("otel.exporter.otlp.headers");
    }

    @Test
    void setsFullMtlsWhenClientCredentialsPresent() throws Exception {
        String certPem = "-----BEGIN CERTIFICATE-----\nCLIENT\n-----END CERTIFICATE-----\n";
        String keyPem = "-----BEGIN PRIVATE KEY-----\nKEY\n-----END PRIVATE KEY-----\n";
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(creds.getString("client-cert")).thenReturn(certPem);
        when(creds.getString("client-key")).thenReturn(keyPem);
        when(certFile.getAbsolutePath()).thenReturn("/tmp/client.crt");
        when(keyFile.getAbsolutePath()).thenReturn("/tmp/client.key");
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);
        when(pemFileCreator.writeFile(eq("gen-client-cert-"), eq(".crt"), eq(certPem))).thenReturn(certFile);
        when(pemFileCreator.writeFile(eq("gen-client-key-"), eq(".key"), eq(keyPem))).thenReturn(keyFile);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.certificate", "/tmp/ca.crt")
                .containsEntry("otel.exporter.otlp.client.certificate", "/tmp/client.crt")
                .containsEntry("otel.exporter.otlp.client.key", "/tmp/client.key")
                .doesNotContainKey("otel.exporter.otlp.headers");
    }

    @Test
    void addsBearerTokenHeaderWhenTokenPresent() throws Exception {
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("auth-token")).thenReturn("secret-token");
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.certificate", "/tmp/ca.crt")
                .containsEntry("otel.exporter.otlp.headers", "Authorization=Bearer secret-token");
    }

    @Test
    void doesNotAddHeaderWhenTokenKeyIsNull() throws Exception {
        BindingCredentialKeys noTokenKeys = BindingCredentialKeys.builder()
                .endpointUrlKey("endpoint")
                .clientCertKey("client-cert")
                .clientKeyKey("client-key")
                .serverCaCertKey("ca-cert")
                .tokenKey(null)
                .serverCaFilePrefix("gen-server-ca-")
                .build();
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);

        Map<String, String> result =
                BindingPropertiesSupplier.builder("my-binding", Optional.of(instance), pemFileCreator, noTokenKeys)
                        .build().get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.certificate", "/tmp/ca.crt")
                .doesNotContainKey("otel.exporter.otlp.headers");
    }

    @Test
    void doesNotCrashWhenPemFileCreatorThrows() throws Exception {
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(pemFileCreator.writeFile(anyString(), anyString(), anyString()))
                .thenThrow(new IOException("disk full"));

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.endpoint", "https://gen.example.com:4318")
                .doesNotContainKey("otel.exporter.otlp.certificate");
    }

    @Test
    void logsWarningWithDescriptionWhenCaMissing() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        Logger bpsLogger = Logger.getLogger(BindingPropertiesSupplier.class.getName());
        bpsLogger.addHandler(handler);
        try {
            when(instance.getCredentials()).thenReturn(creds);
            when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
            when(creds.getString("client-cert")).thenReturn(null);
            when(creds.getString("client-key")).thenReturn(null);
            when(creds.getString("ca-cert")).thenReturn(null);

            newSupplier(Optional.of(instance)).get();

            assertThat(records).anyMatch(r -> r.getLevel() == Level.WARNING
                    && r.getMessage().contains("my-binding")
                    && r.getMessage().contains("server CA"));
        } finally {
            bpsLogger.removeHandler(handler);
        }
    }

    @Test
    void schemePrefixAppliedToUrl() throws Exception {
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("raw-url");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);

        Map<String, String> result = newSupplierWith(Optional.of(instance), "prefix://", null, null, null).get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "prefix://raw-url");
    }

    @Test
    void protocolOverrideApplied() throws Exception {
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);

        Map<String, String> result = newSupplierWith(Optional.of(instance), null, null, "grpc", null).get();

        assertThat(result).containsEntry("otel.exporter.otlp.protocol", "grpc");
    }

    @Test
    void compressionOverrideApplied() throws Exception {
        String caPem = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"), eq(".crt"), eq(caPem))).thenReturn(caFile);

        Map<String, String> result = newSupplierWith(Optional.of(instance), null, null, null, "none").get();

        assertThat(result).containsEntry("otel.exporter.otlp.compression", "none");
    }

    @Test
    void serverCaCertProviderCalledWithTransformedUrlWhenBothClientCredsPresent() throws Exception {
        String clientCertPem = "-----BEGIN CERTIFICATE-----\nC\n-----END CERTIFICATE-----\n";
        String clientKeyPem  = "-----BEGIN PRIVATE KEY-----\nK\n-----END PRIVATE KEY-----\n";
        String caPem         = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        @SuppressWarnings("unchecked")
        Function<String, String> certProvider = mock(Function.class);
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("raw-url");
        when(creds.getString("client-cert")).thenReturn(clientCertPem);
        when(creds.getString("client-key")).thenReturn(clientKeyPem);
        when(certProvider.apply("prefix://raw-url")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(certFile.getAbsolutePath()).thenReturn("/tmp/client.crt");
        when(keyFile.getAbsolutePath()).thenReturn("/tmp/client.key");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"),   eq(".crt"), eq(caPem))).thenReturn(caFile);
        when(pemFileCreator.writeFile(eq("gen-client-cert-"), eq(".crt"), eq(clientCertPem))).thenReturn(certFile);
        when(pemFileCreator.writeFile(eq("gen-client-key-"),  eq(".key"), eq(clientKeyPem))).thenReturn(keyFile);

        Map<String, String> result = BindingPropertiesSupplier
                .builder("my-binding", Optional.of(instance), pemFileCreator, KEYS)
                .scheme("prefix://")
                .serverCaCertProvider(certProvider)
                .build().get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.endpoint", "prefix://raw-url")
                .containsKey("otel.exporter.otlp.certificate")
                .containsKey("otel.exporter.otlp.client.certificate")
                .containsKey("otel.exporter.otlp.client.key");
        verify(certProvider).apply("prefix://raw-url");
    }

    @Test
    void serverCaCertProviderCalledEvenWhenClientCredsMissing() {
        @SuppressWarnings("unchecked")
        Function<String, String> certProvider = mock(Function.class);
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://some-url");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);

        Map<String, String> result = newSupplierWith(Optional.of(instance), null, certProvider, null, null).get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://some-url")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
        verify(certProvider).apply("https://some-url");
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void returnsBasicPropsWhenServerCaCertProviderReturnsNull() {
        @SuppressWarnings("unchecked")
        Function<String, String> certProvider = mock(Function.class);
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://some-url");
        when(creds.getString("client-cert")).thenReturn("cert-pem");
        when(creds.getString("client-key")).thenReturn("key-pem");
        when(certProvider.apply("https://some-url")).thenReturn(null);

        Map<String, String> result = newSupplierWith(Optional.of(instance), null, certProvider, null, null).get();

        assertThat(result).isEmpty();
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void returnsEmptyWhenOnlyOneClientCredentialPresent() {
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://gen.example.com:4318");
        when(creds.getString("client-cert")).thenReturn("cert-pem");
        when(creds.getString("client-key")).thenReturn(null);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result).isEmpty();
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void urlTransformAppliedBeforeSchemeAndPort() throws Exception {
        String clientCertPem = "-----BEGIN CERTIFICATE-----\nC\n-----END CERTIFICATE-----\n";
        String clientKeyPem  = "-----BEGIN PRIVATE KEY-----\nK\n-----END PRIVATE KEY-----\n";
        String caPem         = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("host:<placeholder>");
        when(creds.getString("client-cert")).thenReturn(clientCertPem);
        when(creds.getString("client-key")).thenReturn(clientKeyPem);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(certFile.getAbsolutePath()).thenReturn("/tmp/client.crt");
        when(keyFile.getAbsolutePath()).thenReturn("/tmp/client.key");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"),   eq(".crt"), eq(caPem))).thenReturn(caFile);
        when(pemFileCreator.writeFile(eq("gen-client-cert-"), eq(".crt"), eq(clientCertPem))).thenReturn(certFile);
        when(pemFileCreator.writeFile(eq("gen-client-key-"),  eq(".key"), eq(clientKeyPem))).thenReturn(keyFile);

        Map<String, String> result = BindingPropertiesSupplier
                .builder("my-binding", Optional.of(instance), pemFileCreator, KEYS)
                .urlTransform(u -> u.replace("<placeholder>", "9999"))
                .scheme("https://")
                .build().get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://host:9999");
    }

    @Test
    void serverCaCertRequiresMtlsSkipsProviderWhenNoClientCreds() {
        @SuppressWarnings("unchecked")
        Function<String, String> certProvider = mock(Function.class);
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://some-url");
        when(creds.getString("client-cert")).thenReturn(null);
        when(creds.getString("client-key")).thenReturn(null);

        Map<String, String> result = BindingPropertiesSupplier
                .builder("my-binding", Optional.of(instance), pemFileCreator, KEYS)
                .serverCaCertProvider(certProvider)
                .serverCaCertRequiresMtls(true)
                .build().get();

        assertThat(result).containsEntry("otel.exporter.otlp.endpoint", "https://some-url")
                          .doesNotContainKey("otel.exporter.otlp.certificate");
        verifyNoInteractions(certProvider);
        verifyNoInteractions(pemFileCreator);
    }

    @Test
    void mtlsAndTokenCombinedMode() throws Exception {
        String clientCertPem = "-----BEGIN CERTIFICATE-----\nC\n-----END CERTIFICATE-----\n";
        String clientKeyPem  = "-----BEGIN PRIVATE KEY-----\nK\n-----END PRIVATE KEY-----\n";
        String caPem         = "-----BEGIN CERTIFICATE-----\nCA\n-----END CERTIFICATE-----\n";
        when(instance.getCredentials()).thenReturn(creds);
        when(creds.getString("endpoint")).thenReturn("https://some-url");
        when(creds.getString("client-cert")).thenReturn(clientCertPem);
        when(creds.getString("client-key")).thenReturn(clientKeyPem);
        when(creds.getString("ca-cert")).thenReturn(caPem);
        when(creds.getString("auth-token")).thenReturn("my-bearer-token");
        when(caFile.getAbsolutePath()).thenReturn("/tmp/ca.crt");
        when(certFile.getAbsolutePath()).thenReturn("/tmp/client.crt");
        when(keyFile.getAbsolutePath()).thenReturn("/tmp/client.key");
        when(pemFileCreator.writeFile(eq("gen-server-ca-"),   eq(".crt"), eq(caPem))).thenReturn(caFile);
        when(pemFileCreator.writeFile(eq("gen-client-cert-"), eq(".crt"), eq(clientCertPem))).thenReturn(certFile);
        when(pemFileCreator.writeFile(eq("gen-client-key-"),  eq(".key"), eq(clientKeyPem))).thenReturn(keyFile);

        Map<String, String> result = newSupplier(Optional.of(instance)).get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.endpoint", "https://some-url")
                .containsEntry("otel.exporter.otlp.certificate", "/tmp/ca.crt")
                .containsEntry("otel.exporter.otlp.client.certificate", "/tmp/client.crt")
                .containsEntry("otel.exporter.otlp.client.key", "/tmp/client.key")
                .containsEntry("otel.exporter.otlp.headers", "Authorization=Bearer my-bearer-token");
    }
}
