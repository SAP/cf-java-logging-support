package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurableOtelBindingPropertiesSupplierTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private PemFileCreator pemFileCreator;

    private ConfigProperties config(Map<String, String> entries) {
        ComponentLoader loader = ComponentLoader.forClassLoader(DefaultConfigProperties.class.getClassLoader());
        return DefaultConfigProperties.create(entries, loader);
    }

    private static CloudFoundryServiceInstance collectorInstance() {
        CloudFoundryCredentials credentials = CloudFoundryCredentials.builder()
                .add("url", "https://collector.example.com:4318")
                .add("tls.ca.crt", "ca-pem-content")
                .build();
        return CloudFoundryServiceInstance.builder()
                .name("my-collector")
                .credentials(credentials)
                .build();
    }

    @Test
    void returnsEmptyWhenNoSelectorConfigured() {
        ConfigurableOtelBindingPropertiesSupplier supplier =
                new ConfigurableOtelBindingPropertiesSupplier(adapter, pemFileCreator, config(emptyMap()));

        assertThat(supplier.get()).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoBindingFound() {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.empty());
        ConfigurableOtelBindingPropertiesSupplier supplier =
                new ConfigurableOtelBindingPropertiesSupplier(adapter, pemFileCreator,
                        config(Map.of("sap.otel.generic.cf.binding.name", "my-collector")));

        assertThat(supplier.get()).isEmpty();
    }

    @Test
    void extractsBindingWithDefaultCredentialKeys() throws Exception {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(collectorInstance()));
        when(pemFileCreator.writeFile("otel-generic-server-ca-", ".crt", "ca-pem-content"))
                .thenReturn(new File("ca-file"));

        ConfigurableOtelBindingPropertiesSupplier supplier =
                new ConfigurableOtelBindingPropertiesSupplier(adapter, pemFileCreator,
                        config(Map.of("sap.otel.generic.cf.binding.name", "my-collector")));

        Map<String, String> result = supplier.get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.endpoint", "https://collector.example.com:4318")
                .containsEntry("otel.exporter.otlp.protocol", "http/protobuf")
                .containsEntry("otel.exporter.otlp.compression", "gzip");
    }

    @Test
    void respectsCustomProtocolAndCompression() throws Exception {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(collectorInstance()));
        when(pemFileCreator.writeFile("otel-generic-server-ca-", ".crt", "ca-pem-content"))
                .thenReturn(new File("ca-file"));

        ConfigurableOtelBindingPropertiesSupplier supplier =
                new ConfigurableOtelBindingPropertiesSupplier(adapter, pemFileCreator,
                        config(Map.of(
                                "sap.otel.generic.cf.binding.name", "my-collector",
                                "sap.otel.generic.cf.binding.protocol", "grpc",
                                "sap.otel.generic.cf.binding.compression", "none")));

        Map<String, String> result = supplier.get();

        assertThat(result)
                .containsEntry("otel.exporter.otlp.protocol", "grpc")
                .containsEntry("otel.exporter.otlp.compression", "none");
    }

    @Test
    void findsBindingByLabelSelector() throws Exception {
        CloudFoundryServiceInstance instance = CloudFoundryServiceInstance.builder()
                .name("some-instance")
                .label("my-service-label")
                .credentials(CloudFoundryCredentials.builder()
                        .add("url", "https://collector.example.com:4318")
                        .add("tls.ca.crt", "ca-pem-content")
                        .build())
                .build();
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(pemFileCreator.writeFile("otel-generic-server-ca-", ".crt", "ca-pem-content"))
                .thenReturn(new File("ca-file"));

        ConfigurableOtelBindingPropertiesSupplier supplier =
                new ConfigurableOtelBindingPropertiesSupplier(adapter, pemFileCreator,
                        config(Map.of("sap.otel.generic.cf.binding.label", "my-service-label")));

        assertThat(supplier.get()).containsEntry("otel.exporter.otlp.endpoint", "https://collector.example.com:4318");
    }

    @Test
    void findsBindingByTagSelector() throws Exception {
        CloudFoundryServiceInstance instance = CloudFoundryServiceInstance.builder()
                .name("some-instance")
                .tag("my-tag")
                .credentials(CloudFoundryCredentials.builder()
                        .add("url", "https://collector.example.com:4318")
                        .add("tls.ca.crt", "ca-pem-content")
                        .build())
                .build();
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(instance));
        when(pemFileCreator.writeFile("otel-generic-server-ca-", ".crt", "ca-pem-content"))
                .thenReturn(new File("ca-file"));

        ConfigurableOtelBindingPropertiesSupplier supplier =
                new ConfigurableOtelBindingPropertiesSupplier(adapter, pemFileCreator,
                        config(Map.of("sap.otel.generic.cf.binding.tag", "my-tag")));

        assertThat(supplier.get()).containsEntry("otel.exporter.otlp.endpoint", "https://collector.example.com:4318");
    }
}
