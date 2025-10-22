package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudLoggingSpanExporterProviderTest {

    @Mock
    private Function<ConfigProperties, Stream<CloudFoundryServiceInstance>> servicesProvider;

    @Mock
    private CloudLoggingCredentials.Parser credentialParser;

    @Mock(strictness = LENIENT)
    private ConfigProperties config;

    @InjectMocks
    private CloudLoggingSpanExporterProvider exporterProvider;

    @BeforeEach
    void setUp() {
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });

    }

    @Test
    void canLoadViaSPI() {
        ServiceLoader<ConfigurableSpanExporterProvider> loader =
                ServiceLoader.load(ConfigurableSpanExporterProvider.class);
        Stream<ConfigurableSpanExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(CloudLoggingSpanExporterProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(CloudLoggingSpanExporterProvider.class));
    }

    @Test
    void registersNoopExporterWithoutBindings() {
        when(servicesProvider.apply(config)).thenReturn(Stream.empty());
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithInvalidBindings() {
        when(servicesProvider.apply(config)).thenReturn(Stream.of(CloudFoundryServiceInstance.builder().build()));
        CloudLoggingCredentials cloudLoggingCredentials = mock(CloudLoggingCredentials.class);
        when(credentialParser.parse(any())).thenReturn(cloudLoggingCredentials);
        when(cloudLoggingCredentials.validate()).thenReturn(false);
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersExportersWithValidBindings() throws IOException {
        CloudFoundryServiceInstance genericCfService = CloudFoundryServiceInstance.builder().build();
        CloudFoundryServiceInstance cloudLoggingService = CloudFoundryServiceInstance.builder().build();
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
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpGrpcSpanExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }

}
