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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynatraceMetricsExporterProviderTest {

    @Mock
    private Function<ConfigProperties, CfService> servicesProvider;

    @Mock
    private ConfigProperties config;

    @InjectMocks
    private DynatraceMetricsExporterProvider exporterProvider;

    @BeforeEach
    public void setUp() {
        when(config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.metrics.token-name")).thenReturn("ingest-token");
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });

    }

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ConfigurableMetricExporterProvider> loader = ServiceLoader.load(ConfigurableMetricExporterProvider.class);
        Stream<ConfigurableMetricExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(providers.anyMatch(p -> p instanceof DynatraceMetricsExporterProvider),
                DynatraceMetricsExporterProviderTest.class.getName() + " not loaded via SPI");
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
        CfService genericCfService = new CfService(Collections.emptyMap());
        Mockito.when(servicesProvider.apply(config)).thenReturn(genericCfService);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), containsString("Noop"));
    }

    @Test
    public void registersExportersWithValidBindings() throws IOException {
        Map<String, Object> credentials = new HashMap<String, Object>() {{
            put("apiurl", "https://example.dt/api");
            put("ingest-token", "secret");
        }};
        CfService dynatraceService = new CfService(new HashMap<String, Object>() {{
            put("name", "test-dt");
            put("label", "dynatrace");
            put("credentials", credentials);
        }});
        when(servicesProvider.apply(config)).thenReturn(dynatraceService);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), both(containsString("OtlpHttpMetricExporter")).and(containsString("https://example.dt/api/v2/otlp/v1/metrics,")));
    }

}