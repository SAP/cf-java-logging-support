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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VcapServiceProviderTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private CloudFoundryServiceInstance mockService;

    @BeforeEach
    void setUp() {
        when(adapter.stream(anyList(), anyList(), any())).thenReturn(Stream.of(mockService));
    }

    @Test
    void noFiltersWithDefaultConfig() {
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        VcapServiceProvider provider = new VcapServiceProvider(config, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(Collections.emptyList(), Collections.emptyList(), null);
    }

    @Test
    void customLabel() {
        DefaultConfigProperties config =
                DefaultConfigProperties.createFromMap(Map.of("sap.vcap-service.cf.binding.label.value", "my-label"));
        VcapServiceProvider provider = new VcapServiceProvider(config, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(List.of("my-label"), Collections.emptyList(), null);
    }

    @Test
    void customTag() {
        DefaultConfigProperties config =
                DefaultConfigProperties.createFromMap(Map.of("sap.vcap-service.cf.binding.tag.value", "my-tag"));
        VcapServiceProvider provider = new VcapServiceProvider(config, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(Collections.emptyList(), List.of("my-tag"), null);
    }

    @Test
    void customName() {
        DefaultConfigProperties config =
                DefaultConfigProperties.createFromMap(Map.of("sap.vcap-service.cf.binding.name", "my-service"));
        VcapServiceProvider provider = new VcapServiceProvider(config, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(Collections.emptyList(), Collections.emptyList(), "my-service");
    }

    @Test
    void allFiltersConfigured() {
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(
                Map.of("sap.vcap-service.cf.binding.label.value", "my-label", "sap.vcap-service.cf.binding.tag.value",
                       "my-tag", "sap.vcap-service.cf.binding.name", "my-service"));
        VcapServiceProvider provider = new VcapServiceProvider(config, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(List.of("my-label"), List.of("my-tag"), "my-service");
    }

    @Test
    void returnsNullWhenNoServiceFound() {
        when(adapter.stream(anyList(), anyList(), any())).thenReturn(Stream.empty());
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        VcapServiceProvider provider = new VcapServiceProvider(config, adapter);

        assertThat(provider.get()).isNull();
    }
}
