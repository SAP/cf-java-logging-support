package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.pivotal.cfenv.core.CfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CloudLoggingSpanExporterProviderTest {

    @Mock
    private Function<ConfigProperties, Stream<CfService>> servicesProvider;

    @Mock
    private CloudLoggingCredentials.Parser credentialParser;

    @Mock
    private ConfigProperties config;

    @InjectMocks
    private CloudLoggingSpanExporterProvider exporterProvider;

    @BeforeEach
    public void setUp() {
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });

    }

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ConfigurableSpanExporterProvider> loader = ServiceLoader.load(ConfigurableSpanExporterProvider.class);
        Stream<ConfigurableSpanExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(providers.anyMatch(p -> p instanceof CloudLoggingSpanExporterProvider),
                CloudLoggingSpanExporterProvider.class.getName() + " not loaded via SPI");
    }

    @Test
    public void registersNoopExporterWithoutBindings() {
        when(servicesProvider.apply(config)).thenReturn(Stream.empty());
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), containsString("Noop"));
    }

    @Test
    public void registersNoopExporterWithInvalidBindings() {
        CfService genericCfService = new CfService(Collections.emptyMap());
        when(servicesProvider.apply(config)).thenReturn(Stream.of(genericCfService));
        CloudLoggingCredentials cloudLoggingCredentials = mock(CloudLoggingCredentials.class);
        when(credentialParser.parse(any())).thenReturn(cloudLoggingCredentials);
        when(cloudLoggingCredentials.validate()).thenReturn(false);
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), containsString("Noop"));
    }

    @Test
    public void registersExportersWithValidBindings() throws IOException {
        CfService genericCfService = new CfService(Collections.emptyMap());
        CfService cloudLoggingService = new CfService(Collections.emptyMap());
        when(servicesProvider.apply(config)).thenReturn(Stream.of(genericCfService, cloudLoggingService));
        CloudLoggingCredentials invalidCredentials = mock(CloudLoggingCredentials.class);
        when(invalidCredentials.validate()).thenReturn(false);
        CloudLoggingCredentials validCredentials = mock(CloudLoggingCredentials.class);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(credentialParser.parse(any())).thenReturn(invalidCredentials).thenReturn(validCredentials);
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter, is(notNullValue()));
        assertThat(exporter.toString(), both(containsString("OtlpGrpcSpanExporter")).and(containsString("https://otlp-example.sap")));
    }

}