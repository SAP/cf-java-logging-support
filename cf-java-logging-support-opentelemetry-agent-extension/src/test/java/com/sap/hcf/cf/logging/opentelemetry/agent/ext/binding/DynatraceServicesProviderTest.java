package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.pivotal.cfenv.core.CfService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynatraceServicesProviderTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private CfService mockService;

    @Before
    public void setUp() throws Exception {
        when(adapter.stream(anyListOf(String.class), anyListOf(String.class))).thenReturn(Stream.of(mockService));
    }

    @Test
    public void defaultLabelsAndTags() {
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        DynatraceServiceProvider provider = new DynatraceServiceProvider(emptyProperties, adapter);

        assertThat(provider.get(), is(mockService));
        verify(adapter).stream(asList("user-provided", "dynatrace"), Collections.singletonList("dynatrace"));
    }

    @Test
    public void customLabel() {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.binding.dynatrace.label", "not-dynatrace");
        properties.put("otel.javaagent.extension.sap.cf.binding.user-provided.label", "unknown-label");
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(properties);
        DynatraceServiceProvider provider = new DynatraceServiceProvider(config, adapter);

        assertThat(provider.get(), is(mockService));
        verify(adapter).stream(asList("unknown-label", "not-dynatrace"), Collections.singletonList("dynatrace"));
    }

    @Test
    public void customTag() {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.binding.dynatrace.tag", "NOT dynatrace");
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(properties);
        DynatraceServiceProvider provider = new DynatraceServiceProvider(emptyProperties, adapter);

        assertThat(provider.get(), is(mockService));
        verify(adapter).stream(asList("user-provided", "dynatrace"), Collections.singletonList("NOT dynatrace"));
    }

}