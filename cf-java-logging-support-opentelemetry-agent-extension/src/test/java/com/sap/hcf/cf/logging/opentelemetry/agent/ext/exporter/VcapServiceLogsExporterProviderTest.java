package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
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
public class VcapServiceLogsExporterProviderTest {

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

    private VcapServiceLogsExporterProvider exporterProvider;

    @BeforeEach
    void setUp() {
        when(config.getString(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[1];
            }
        });
        exporterProvider = new VcapServiceLogsExporterProvider(serviceProvider, credentialParserProvider,
                                                               serverCertificateDownloader);
    }

    @Test
    void canLoadViaSPI() {
        ServiceLoader<ConfigurableLogRecordExporterProvider> loader =
                ServiceLoader.load(ConfigurableLogRecordExporterProvider.class);
        Stream<ConfigurableLogRecordExporterProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(VcapServiceLogsExporterProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(VcapServiceLogsExporterProvider.class));
    }

    @Test
    void registersNoopExporterWithoutBinding() {
        when(serviceProvider.apply(config)).thenReturn(null);
        LogRecordExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("Noop");
    }

    @Test
    void registersNoopExporterWithoutCredentials() {
        CloudFoundryServiceInstance serviceInstance =
                CloudFoundryServiceInstance.builder().name("test-service").label("test-label").build();
        when(serviceProvider.apply(config)).thenReturn(serviceInstance);
        LogRecordExporter exporter = exporterProvider.createExporter(config);
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
        LogRecordExporter exporter = exporterProvider.createExporter(config);
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
        when(config.getString("otel.exporter.vcap-service.logs.protocol")).thenReturn("unsupported");
        LogRecordExporter exporter = exporterProvider.createExporter(config);
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
        LogRecordExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpLogRecordExporter")
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
        when(config.getString("otel.exporter.vcap-service.logs.protocol")).thenReturn("grpc");
        LogRecordExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpGrpcLogRecordExporter")
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
        LogRecordExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpLogRecordExporter")
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
        LogRecordExporter exporter = exporterProvider.createExporter(config);
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
        LogRecordExporter exporter = exporterProvider.createExporter(config);
        assertThat(exporter).isNotNull();
        assertThat(exporter.toString()).containsSubsequence("OtlpHttpLogRecordExporter")
                                       .containsSubsequence("https://otlp-example.sap");
    }
}
