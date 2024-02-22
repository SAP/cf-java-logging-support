package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.pivotal.cfenv.core.CfService;
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
import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynatraceMetricsExporterProviderTest {

    @Mock
    private Function<ConfigProperties, CfService> servicesProvider;

    @Mock(strictness = LENIENT)
    private ConfigProperties config;

    @InjectMocks
    private DynatraceMetricsExporterProvider exporterProvider;

    @BeforeEach
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
        assertThat(providers).describedAs(DynatraceMetricsExporterProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(DynatraceMetricsExporterProvider.class));
    }

    @Test
    public void registersNoopExporterWithoutBindings() {
        when(servicesProvider.apply(config)).thenReturn(null);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    public void registersNoopExporterWithInvalidBindings() {
        CfService genericCfService = new CfService(Collections.emptyMap());
        Mockito.when(servicesProvider.apply(config)).thenReturn(genericCfService);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    public void registersExportersWithValidBindings() throws IOException {
        Map<String, Object> credentials =
                Map.ofEntries(entry("apiurl", "https://example.dt/api"), entry("ingest-token", "secret"));
        CfService dynatraceService = new CfService(Map.ofEntries(entry("name", "test-dt"), entry("label", "dynatrace"),
                                                                 entry("credentials", credentials)));
        when(servicesProvider.apply(config)).thenReturn(dynatraceService);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpMetricExporter")
                                       .containsSubsequence("https://example.dt/api/v2/otlp/v1/metrics,");
    }

}
