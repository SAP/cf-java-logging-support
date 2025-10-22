package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynatraceMetricsExporterProviderTest {

    @Mock
    private Function<ConfigProperties, CloudFoundryServiceInstance> servicesProvider;

    @Mock(strictness = LENIENT)
    private ConfigProperties config;

    @InjectMocks
    private DynatraceMetricsExporterProvider exporterProvider;

    @BeforeEach
    void setUp() {
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
    void canLoadViaSPI() {
        ServiceLoader<ConfigurableMetricExporterProvider> loader =
                ServiceLoader.load(ConfigurableMetricExporterProvider.class);
        Stream<ConfigurableMetricExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(DynatraceMetricsExporterProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(DynatraceMetricsExporterProvider.class));
    }

    @Test
    void registersNoopExporterWithoutBindings() {
        when(servicesProvider.apply(config)).thenReturn(null);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithInvalidBindings() {
        Mockito.when(servicesProvider.apply(config)).thenReturn(CloudFoundryServiceInstance.builder().build());
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithInvalidBindingsFromEmptyCredentials() {
        Mockito.when(servicesProvider.apply(config)).thenReturn(
                CloudFoundryServiceInstance.builder().credentials(CloudFoundryCredentials.builder().build()).build());
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersExportersWithValidBindings() throws IOException {
        CloudFoundryServiceInstance dynatraceServiceInstance =
                CloudFoundryServiceInstance.builder().name("test-dt").label("dynatrace").credentials(
                        CloudFoundryCredentials.builder().add("apiurl", "https://example.dt/api")
                                               .add("ingest-token", "secret").build()).build();
        when(servicesProvider.apply(config)).thenReturn(dynatraceServiceInstance);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpMetricExporter")
                                       .containsSubsequence("https://example.dt/api/v2/otlp/v1/metrics,");
    }

}
