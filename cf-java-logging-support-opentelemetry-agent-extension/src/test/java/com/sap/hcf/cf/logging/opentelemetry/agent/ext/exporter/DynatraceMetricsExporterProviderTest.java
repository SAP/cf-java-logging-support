package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynatraceMetricsExporterProviderTest {

    @Mock
    private Function<ConfigProperties, CloudFoundryServiceInstance> servicesProvider;

    @Mock
    private ConfigProperties config;

    @InjectMocks
    private DynatraceMetricsExporterProvider exporterProvider;

    @Before
    public void setUp() {
        when(config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.metrics.token-name")).thenReturn(
                "ingest-token");
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });

    }

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ConfigurableMetricExporterProvider> loader =
                ServiceLoader.load(ConfigurableMetricExporterProvider.class);
        Stream<ConfigurableMetricExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(DynatraceMetricsExporterProviderTest.class.getName() + " not loaded via SPI",
                   providers.anyMatch(p -> p instanceof DynatraceMetricsExporterProvider));
    }

    @Test
    public void registersNoopExporterWithoutBindings() {
        when(servicesProvider.apply(config)).thenReturn(null);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), containsString("Noop"));
    }

    @Test
    public void registersNoopExporterWithInvalidBindings() {
        Mockito.when(servicesProvider.apply(config)).thenReturn(CloudFoundryServiceInstance.builder().build());
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), containsString("Noop"));
    }

    @Test
    public void registersNoopExporterWithInvalidBindingsFromEmptyCredentials() {
        Mockito.when(servicesProvider.apply(config)).thenReturn(
                CloudFoundryServiceInstance.builder().credentials(CloudFoundryCredentials.builder().build()).build());
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), containsString("Noop"));
    }

    @Test
    public void registersExportersWithValidBindings() throws IOException {
        CloudFoundryServiceInstance dynatraceServiceInstance =
                CloudFoundryServiceInstance.builder().name("test-dt").label("dynatrace").credentials(
                        CloudFoundryCredentials.builder().add("apiurl", "https://example.dt/api")
                                               .add("ingest-token", "secret").build()).build();
        when(servicesProvider.apply(config)).thenReturn(dynatraceServiceInstance);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), both(containsString("OtlpHttpMetricExporter")).and(
                containsString("https://example.dt/api/v2/otlp/v1/metrics,")));
    }

}
