package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
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
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudLoggingServicesProviderTest {

    @Mock
    private CloudFoundryServicesAdapter adapter;

    @Mock
    private CloudFoundryServiceInstance mockService;

    @Before
    public void setUp() throws Exception {
        when(adapter.stream(anyListOf(String.class), anyListOf(String.class))).thenReturn(Stream.of(mockService));
    }

    @Test
    public void defaultLabelsAndTags() {
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        CloudLoggingServicesProvider provider = new CloudLoggingServicesProvider(emptyProperties, adapter);

        assertThat(provider.get().collect(toList()), contains(mockService));
        verify(adapter).stream(asList("user-provided", "cloud-logging"), Collections.singletonList("Cloud Logging"));
    }

    @Test
    public void customLabel() {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.binding.cloud-logging.label", "not-cloud-logging");
        properties.put("otel.javaagent.extension.sap.cf.binding.user-provided.label", "unknown-label");
        DefaultConfigProperties config = DefaultConfigProperties.createFromMap(properties);
        CloudLoggingServicesProvider provider = new CloudLoggingServicesProvider(config, adapter);

        assertThat(provider.get().collect(toList()), contains(mockService));
        verify(adapter).stream(asList("unknown-label", "not-cloud-logging"),
                               Collections.singletonList("Cloud Logging"));
    }

    @Test
    public void customTag() {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.binding.cloud-logging.tag", "NOT Cloud Logging");
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(properties);
        CloudLoggingServicesProvider provider = new CloudLoggingServicesProvider(emptyProperties, adapter);

        assertThat(provider.get().collect(toList()), contains(mockService));
        verify(adapter).stream(asList("user-provided", "cloud-logging"),
                               Collections.singletonList("NOT Cloud Logging"));
    }

}
