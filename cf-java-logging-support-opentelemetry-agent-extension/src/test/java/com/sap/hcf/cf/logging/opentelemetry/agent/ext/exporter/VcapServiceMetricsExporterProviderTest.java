package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VcapServiceMetricsExporterProviderTest {

    @Mock
    private Function<ConfigProperties, CloudFoundryServiceInstance> serviceProvider;

    @Mock
    private Function<ConfigProperties, Function<CloudFoundryCredentials, VcapServiceCredentials>>
            credentialParserProvider;

    @Mock
    private Function<CloudFoundryCredentials, VcapServiceCredentials> credentialParser;

    @Mock
    private ServerCertificateDownloader serverCertificateDownloader;

    @Mock(strictness = LENIENT)
    private ConfigProperties config;

    private VcapServiceMetricsExporterProvider exporterProvider;

    @BeforeEach
    void setUp() {
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });
        exporterProvider = new VcapServiceMetricsExporterProvider(serviceProvider, credentialParserProvider,
                                                                  serverCertificateDownloader);
    }

    @Test
    void canLoadViaSPI() {
        ServiceLoader<ConfigurableMetricExporterProvider> loader =
                ServiceLoader.load(ConfigurableMetricExporterProvider.class);
        Stream<ConfigurableMetricExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(VcapServiceMetricsExporterProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(VcapServiceMetricsExporterProvider.class));
    }

    @Test
    void hasNameVcapService() {
        assertThat(exporterProvider.getName()).isEqualTo("vcap-service");
    }

    @Test
    void registersNoopExporterWithoutBinding() {
        when(serviceProvider.apply(config)).thenReturn(null);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithoutCredentials() {
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label").build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithInvalidCredentials() {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials invalidCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(invalidCredentials);
        when(invalidCredentials.validate()).thenReturn(false);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithUnsupportedProtocol() {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(config.getString("otel.exporter.vcap-service.metrics.protocol")).thenReturn("unsupported");
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersHttpProtobufExporterWithTlsCredentials() throws IOException {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(PEMUtil.read("certificate.pem"));
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpMetricExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }

    @Test
    void registersGrpcExporterWithTlsCredentials() throws IOException {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(config.getString("otel.exporter.vcap-service.metrics.protocol")).thenReturn("grpc");
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpGrpcMetricExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }

    @Test
    void registersExporterWhenServerCertIsDownloaded() throws IOException {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(null);
        when(serverCertificateDownloader.download("https://otlp-example.sap")).thenReturn(
                new String(PEMUtil.read("certificate.pem")));
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpMetricExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }

    @Test
    void registersNoopExporterWhenServerCertDownloadFails() throws IOException {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(null);
        when(serverCertificateDownloader.download("https://otlp-example.sap")).thenReturn(null);
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersExporterWithAuthToken() {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientKey()).thenReturn(null);
        when(validCredentials.getAuthToken()).thenReturn("test-auth-token");
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpMetricExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }

    @Test
    void wrapsExporterWithFilteringWhenIncludeNamesConfigured() throws IOException {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(config.getList("otel.exporter.vcap-service.metrics.include.names")).thenReturn(
                List.of("jvm.memory.used", "jvm.cpu*"));
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isInstanceOf(FilteringMetricExporter.class);
    }

    @Test
    void wrapsExporterWithFilteringWhenExcludeNamesConfigured() throws IOException {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(validCredentials.getClientCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(validCredentials.getClientKey()).thenReturn(PEMUtil.read("private.pem"));
        when(validCredentials.getServerCert()).thenReturn(PEMUtil.read("certificate.pem"));
        when(config.getList("otel.exporter.vcap-service.metrics.exclude.names")).thenReturn(List.of("jvm.gc*"));
        MetricExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isInstanceOf(FilteringMetricExporter.class);
    }

    @Test
    void throwsOnUnrecognizedTemporalityPreference() {
        CloudFoundryCredentials rawCredentials = CloudFoundryCredentials.builder().build();
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label")
                                           .credentials(rawCredentials).build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        when(credentialParserProvider.apply(config)).thenReturn(credentialParser);
        VcapServiceCredentials validCredentials = mock(VcapServiceCredentials.class);
        when(credentialParser.apply(any())).thenReturn(validCredentials);
        when(validCredentials.validate()).thenReturn(true);
        when(validCredentials.getEndpoint()).thenReturn("https://otlp-example.sap");
        when(config.getString("otel.exporter.vcap-service.metrics.temporality.preference")).thenReturn("unknown");
        assertThatThrownBy(() -> exporterProvider.createExporter(config)).isInstanceOf(ConfigurationException.class)
                                                                          .hasMessageContaining("unknown");
    }
}
