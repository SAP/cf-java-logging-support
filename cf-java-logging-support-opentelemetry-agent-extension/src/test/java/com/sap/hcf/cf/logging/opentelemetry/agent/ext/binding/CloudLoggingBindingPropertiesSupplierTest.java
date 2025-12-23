package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudLoggingBindingPropertiesSupplierTest {

    private static final Map<String, Object> CREDENTIALS =
            Map.ofEntries(entry("ingest-otlp-endpoint", "test-endpoint"), entry("ingest-otlp-key", "test-client-key"),
                          entry("ingest-otlp-cert", "test-client-cert"), entry("server-ca", "test-server-cert"));

    private static final Map<String, Object> BINDING =
            Map.ofEntries(entry("label", "user-provided"), entry("name", "test-name"),
                          entry("tags", List.of("Cloud Logging")), entry("credentials", CREDENTIALS));

    @Mock
    private CloudLoggingServicesProvider servicesProvider;

    @Mock
    private PemFileCreator pemFileCreator;

    @InjectMocks
    private CloudLoggingBindingPropertiesSupplier propertiesSupplier;

    @AfterEach
    void assertNoUnexpectedInteractions() {
        verifyNoMoreInteractions(servicesProvider, pemFileCreator);
    }

    @Test
    void emptyWithoutBindings() {
        when(servicesProvider.get()).thenReturn(Stream.empty());
        Map<String, String> properties = propertiesSupplier.get();
        assertThat(properties).isEmpty();
    }

    @Test
    void extractsBinding() throws Exception {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", "test-client-key")
                                       .add("ingest-otlp-cert", "test-client-cert").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        when(pemFileCreator.writeFile("cloud-logging-client", ".key", "test-client-key")).thenReturn(
                new File("client-key-file"));
        when(pemFileCreator.writeFile("cloud-logging-client", ".cert", "test-client-cert")).thenReturn(
                new File("client-cert-file"));
        when(pemFileCreator.writeFile("cloud-logging-server", ".cert", "test-server-cert")).thenReturn(
                new File("server-cert-file"));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider, pemFileCreator);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).containsEntry("otel.exporter.otlp.endpoint", "https://test-endpoint")
                              .containsKey("otel.exporter.otlp.client.key")
                              .containsKey("otel.exporter.otlp.client.certificate")
                              .containsKey("otel.exporter.otlp.certificate");
    }

    private static CloudFoundryServiceInstance.Builder defaultInstance() {
        return CloudFoundryServiceInstance.builder().name("test-name").label("user-provided").tag("Cloud Logging");
    }

    @Test
    void emptyWithoutEndpoint() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-key", "test-client-key")
                                       .add("ingest-otlp-cert", "test-client-cert").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider, pemFileCreator);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).isEmpty();
    }

    @Test
    void emptyWithoutClientCert() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", "test-client-key").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider, pemFileCreator);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).isEmpty();
    }

    @Test
    void emptyWithoutClientKey() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-cert", "test-client-cert").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider, pemFileCreator);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).isEmpty();
    }

    @Test
    void emptyWithoutServerCert() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", "test-client-key")
                                       .add("ingest-otlp-cert", "test-client-cert").build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider, pemFileCreator);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).isEmpty();
    }
}
