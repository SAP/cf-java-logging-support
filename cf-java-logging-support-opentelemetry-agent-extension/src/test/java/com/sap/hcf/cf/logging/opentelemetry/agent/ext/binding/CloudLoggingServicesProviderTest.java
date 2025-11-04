package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudLoggingServicesProviderTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private CloudFoundryServiceInstance mockService;

    @BeforeEach
    void setUp() throws Exception {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(mockService));
    }

    @Test
    void defaultLabelsAndTags() {
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        CloudLoggingServicesProvider provider = new CloudLoggingServicesProvider(emptyProperties, adapter);

        assertThat(provider.get()).containsExactly(mockService);
        verify(adapter).stream(List.of("user-provided", "cloud-logging"), List.of("Cloud Logging"));
    }

    @Test
    void customLabel() {
        Map<String, String> properties =
                Map.ofEntries(entry("otel.javaagent.extension.sap.cf.binding.cloud-logging.label", "not-cloud-logging"),
                              entry("otel.javaagent.extension.sap.cf.binding.user-provided.label", "unknown-label"));
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(properties);
        CloudLoggingServicesProvider provider = new CloudLoggingServicesProvider(config, adapter);

        assertThat(provider.get()).containsExactly(mockService);
        verify(adapter).stream(List.of("unknown-label", "not-cloud-logging"), List.of("Cloud Logging"));
    }

    @Test
    void customTag() {
        Map<String, String> properties =
                Map.ofEntries(entry("otel.javaagent.extension.sap.cf.binding.cloud-logging.tag", "NOT Cloud Logging"));
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(properties);
        CloudLoggingServicesProvider provider = new CloudLoggingServicesProvider(emptyProperties, adapter);

        assertThat(provider.get()).containsExactly(mockService);
        verify(adapter).stream(List.of("user-provided", "cloud-logging"), List.of("NOT Cloud Logging"));
    }

}
