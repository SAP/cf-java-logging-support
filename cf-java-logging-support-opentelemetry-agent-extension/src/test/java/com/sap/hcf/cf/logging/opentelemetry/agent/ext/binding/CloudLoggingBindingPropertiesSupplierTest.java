package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudLoggingBindingPropertiesSupplierTest {

    private static final Map<String, Object> CREDENTIALS = Collections.unmodifiableMap(new HashMap<String, Object>() {{
        put("ingest-otlp-endpoint", "test-endpoint");
        put("ingest-otlp-key", "test-client-key");
        put("ingest-otlp-cert", "test-client-cert");
        put("server-ca", "test-server-cert");
    }});

    private static final Map<String, Object> BINDING = Collections.unmodifiableMap(new HashMap<String, Object>() {{
        put("label", "user-provided");
        put("name", "test-name");
        put("tags", Collections.singletonList("Cloud Logging"));
        put("credentials", CREDENTIALS);
    }});

    @Mock
    private CloudLoggingServicesProvider servicesProvider;

    @InjectMocks
    private CloudLoggingBindingPropertiesSupplier propertiesSupplier;

    private static void assertFileContent(String expected, String filename) throws IOException {
        String contents = Files.readAllLines(Paths.get(filename)).stream().collect(Collectors.joining("\n"));
        assertThat(contents, is(equalTo(expected)));
    }
    
    @Test
    public void emptyWithoutBindings() {
        when(servicesProvider.get()).thenReturn(Stream.empty());
        Map<String, String> properties = propertiesSupplier.get();
        assertTrue(properties.isEmpty());
    }

    @Test
    public void extractsBinding() throws Exception {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", "test-client-key")
                                       .add("ingest-otlp-cert", "test-client-cert").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertThat(properties, hasEntry("otel.exporter.otlp.endpoint", "https://test-endpoint"));
        assertThat(properties, hasKey("otel.exporter.otlp.client.key"));
        assertFileContent("test-client-key", properties.get("otel.exporter.otlp.client.key"));
        assertThat(properties, hasKey("otel.exporter.otlp.client.key"));
        assertFileContent("test-client-key", properties.get("otel.exporter.otlp.client.key"));
        assertThat(properties, hasKey("otel.exporter.otlp.client.certificate"));
        assertFileContent("test-client-cert", properties.get("otel.exporter.otlp.client.certificate"));
        assertThat(properties, hasKey("otel.exporter.otlp.certificate"));
        assertFileContent("test-server-cert", properties.get("otel.exporter.otlp.certificate"));
    }

    private static CloudFoundryServiceInstance.Builder defaultInstance() {
        return CloudFoundryServiceInstance.builder().name("test-name").label("user-provided").tag("Cloud Logging");
    }

    @Test
    public void emptyWithoutEndpoint() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-key", "test-client-key")
                                       .add("ingest-otlp-cert", "test-client-cert").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertTrue(properties.isEmpty());
    }

    @Test
    public void emptyWithoutClientCert() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", "test-client-key").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertTrue(properties.isEmpty());
    }

    @Test
    public void emptyWithoutClientKey() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-cert", "test-client-cert").add("server-ca", "test-server-cert")
                                       .build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertTrue(properties.isEmpty());
    }

    @Test
    public void emptyWithoutServerCert() {
        CloudFoundryCredentials credentials =
                CloudFoundryCredentials.builder().add("ingest-otlp-endpoint", "test-endpoint")
                                       .add("ingest-otlp-key", "test-client-key")
                                       .add("ingest-otlp-cert", "test-client-cert").build();
        when(servicesProvider.get()).thenReturn(Stream.of(defaultInstance().credentials(credentials).build()));
        CloudLoggingBindingPropertiesSupplier propertiesSupplier =
                new CloudLoggingBindingPropertiesSupplier(servicesProvider);

        Map<String, String> properties = propertiesSupplier.get();

        assertTrue(properties.isEmpty());
    }
}
