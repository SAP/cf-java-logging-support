package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.pivotal.cfenv.core.CfEnv;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

public class CloudLoggingBindingPropertiesSupplierTest {

    private static final String VALID_CREDENTIALS = "{\"ingest-otlp-endpoint\":\"test-endpoint\", \"ingest-otlp-key\":\"test-client-key\", \"ingest-otlp-cert\":\"test-client-cert\", \"server-ca\":\"test-server-cert\"}";
    private static final String USER_PROVIDED_VALID = "{\"label\":\"user-provided\", \"name\":\"test-name\", \"tags\":[\"Cloud Logging\"], \"credentials\":" + VALID_CREDENTIALS + "}";
    private static final String MANAGED_VALID = "{\"label\":\"cloud-logging\", \"name\":\"test-name\", \"tags\":[\"Cloud Logging\"], \"credentials\":" + VALID_CREDENTIALS + "}";

    private static void assertFileContent(String expected, String filename) throws IOException {
        String contents = Files.readAllLines(Paths.get(filename))
                .stream()
                .collect(Collectors.joining("\n"));
        assertThat(contents, is(equalTo(expected)));
    }

    @Test
    public void emptyWithoutBindings() {
        CfEnv cfEnv = new CfEnv("", "");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
        Map<String, String> properties = propertiesSupplier.get();
        assertTrue(properties.isEmpty());
    }

    @Test
    public void extractsUserProvidedBinding() throws Exception {
        CfEnv cfEnv = new CfEnv("", "{\"user-provided\":[" + USER_PROVIDED_VALID + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
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

    @Test
    public void extractsManagedBinding() throws Exception {
        CfEnv cfEnv = new CfEnv("", "{\"cloud-logging\":[" + MANAGED_VALID + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
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

    @Test
    public void prefersUserProvidedOverManaged() throws Exception {
        String markedService = USER_PROVIDED_VALID.replace("test-endpoint", "user-endpoint");
        CfEnv cfEnv = new CfEnv("", "{\"cloud-logging\":[" + MANAGED_VALID + "], \"user-provided\":[" + markedService + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
        Map<String, String> properties = propertiesSupplier.get();
        assertThat(properties, hasEntry("otel.exporter.otlp.endpoint", "https://user-endpoint"));
        assertThat(properties, hasKey("otel.exporter.otlp.client.key"));
        assertFileContent("test-client-key", properties.get("otel.exporter.otlp.client.key"));
        assertThat(properties, hasKey("otel.exporter.otlp.client.key"));
        assertFileContent("test-client-key", properties.get("otel.exporter.otlp.client.key"));
        assertThat(properties, hasKey("otel.exporter.otlp.client.certificate"));
        assertFileContent("test-client-cert", properties.get("otel.exporter.otlp.client.certificate"));
        assertThat(properties, hasKey("otel.exporter.otlp.certificate"));
        assertFileContent("test-server-cert", properties.get("otel.exporter.otlp.certificate"));
    }

    @Test
    public void emptyWithoutEndpoint() {
        String markedService = USER_PROVIDED_VALID.replace("test-endpoint", "");
        CfEnv cfEnv = new CfEnv("", "{\"user-provided\":[" + markedService + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
        Map<String, String> properties = propertiesSupplier.get();
        assertTrue(properties.isEmpty());
    }

    @Test
    public void emptyWithoutClientCert() {
        String markedService = USER_PROVIDED_VALID.replace("test-client-cert", "");
        CfEnv cfEnv = new CfEnv("", "{\"user-provided\":[" + markedService + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
        Map<String, String> properties = propertiesSupplier.get();
        assertTrue(properties.isEmpty());
    }

    @Test
    public void emptyWithoutClientKey() {
        String markedService = USER_PROVIDED_VALID.replace("test-client-key", "");
        CfEnv cfEnv = new CfEnv("", "{\"user-provided\":[" + markedService + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
        Map<String, String> properties = propertiesSupplier.get();
        assertTrue(properties.isEmpty());
    }

    @Test
    public void emptyWithoutServerCert() {
        String markedService = USER_PROVIDED_VALID.replace("test-server-cert", "");
        CfEnv cfEnv = new CfEnv("", "{\"user-provided\":[" + markedService + "]}");
        CloudLoggingBindingPropertiesSupplier propertiesSupplier = new CloudLoggingBindingPropertiesSupplier(cfEnv);
        Map<String, String> properties = propertiesSupplier.get();
        assertTrue(properties.isEmpty());
    }
}