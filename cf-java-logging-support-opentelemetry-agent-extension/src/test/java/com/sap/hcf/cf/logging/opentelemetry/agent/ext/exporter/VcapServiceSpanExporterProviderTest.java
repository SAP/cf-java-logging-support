package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VcapServiceSpanExporterProviderTest {

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

    private VcapServiceSpanExporterProvider exporterProvider;

    @BeforeEach
    void setUp() {
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });
        exporterProvider = new VcapServiceSpanExporterProvider(serviceProvider, credentialParserProvider,
                                                               serverCertificateDownloader);
    }

    @Test
    void canLoadViaSPI() {
        ServiceLoader<ConfigurableSpanExporterProvider> loader =
                ServiceLoader.load(ConfigurableSpanExporterProvider.class);
        Stream<ConfigurableSpanExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(VcapServiceSpanExporterProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(VcapServiceSpanExporterProvider.class));
    }

    @Test
    void hasNameVcapService() {
        assertThat(exporterProvider.getName()).isEqualTo("vcap-service");
    }

    @Test
    void registersNoopExporterWithoutBinding() {
        when(serviceProvider.apply(config)).thenReturn(null);
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithoutCredentials() {
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label").build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        SpanExporter exporter = exporterProvider.createExporter(config);
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
        SpanExporter exporter = exporterProvider.createExporter(config);
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
        when(config.getString("otel.exporter.vcap-service.traces.protocol")).thenReturn("unsupported");
        SpanExporter exporter = exporterProvider.createExporter(config);
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
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpSpanExporter")
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
        when(config.getString("otel.exporter.vcap-service.traces.protocol")).thenReturn("grpc");
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpGrpcSpanExporter")
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
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpSpanExporter")
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
        SpanExporter exporter = exporterProvider.createExporter(config);
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
        SpanExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpSpanExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }
}
