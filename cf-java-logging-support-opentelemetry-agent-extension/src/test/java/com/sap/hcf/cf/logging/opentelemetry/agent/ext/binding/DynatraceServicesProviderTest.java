package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.pivotal.cfenv.core.CfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynatraceServicesProviderTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private CfService mockService;

    @BeforeEach
    public void setUp() throws Exception {
        when(adapter.stream(anyList(), anyList())).thenReturn(Stream.of(mockService));
    }

    @Test
    public void defaultLabelsAndTags() {
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        DynatraceServiceProvider provider = new DynatraceServiceProvider(emptyProperties, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(asList("user-provided", "dynatrace"), Collections.singletonList("dynatrace"));
    }

    @Test
    public void customLabel() {
        Map<String, String> properties =
                Map.ofEntries(entry("otel.javaagent.extension.sap.cf.binding.dynatrace.label", "not-dynatrace"),
                              entry("otel.javaagent.extension.sap.cf.binding.user-provided.label", "unknown-label"));
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(properties);
        DynatraceServiceProvider provider = new DynatraceServiceProvider(config, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(asList("unknown-label", "not-dynatrace"), Collections.singletonList("dynatrace"));
    }

    @Test
    public void customTag() {
        Map<String, String> properties =
                Map.ofEntries(entry("otel.javaagent.extension.sap.cf.binding.dynatrace.tag", "NOT dynatrace"));
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(properties);
        DynatraceServiceProvider provider = new DynatraceServiceProvider(emptyProperties, adapter);

        assertThat(provider.get()).isEqualTo(mockService);
        verify(adapter).stream(asList("user-provided", "dynatrace"), Collections.singletonList("NOT dynatrace"));
    }

}
