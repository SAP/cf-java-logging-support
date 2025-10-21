package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.assertj.core.api.AbstractStringAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudLoggingBindingPropertiesSupplierTest {

    private static final Map<String, Object> CREDENTIALS =
            Map.ofEntries(entry("ingest-otlp-endpoint", "test-endpoint"), entry("ingest-otlp-key", "test-client-key"),
                          entry("ingest-otlp-cert", "test-client-cert"), entry("server-ca", "test-server-cert"));

    private static final Map<String, Object> BINDING =
            Map.ofEntries(entry("label", "user-provided"), entry("name", "test-name"),
                          entry("tags", Collections.singletonList("Cloud Logging")), entry("credentials", CREDENTIALS));

    @Mock
    private CloudLoggingServicesProvider servicesProvider;

    @InjectMocks
    private CloudLoggingBindingPropertiesSupplier propertiesSupplier;

    @NotNull
    private static AbstractStringAssert<?> assertFileContent(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        return assertThat(String.join("\n", lines));
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
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).containsEntry("otel.exporter.otlp.endpoint", "https://test-endpoint")
                              .containsKey("otel.exporter.otlp.client.key")
                              .containsKey("otel.exporter.otlp.client.certificate")
                              .containsKey("otel.exporter.otlp.certificate");
        assertFileContent(properties.get("otel.exporter.otlp.client.key")).isEqualTo("test-client-key");
        assertFileContent(properties.get("otel.exporter.otlp.client.certificate")).isEqualTo("test-client-cert");
        assertFileContent(properties.get("otel.exporter.otlp.certificate")).isEqualTo("test-server-cert");
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
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

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
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

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
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

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
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties).isEmpty();
    }
}
