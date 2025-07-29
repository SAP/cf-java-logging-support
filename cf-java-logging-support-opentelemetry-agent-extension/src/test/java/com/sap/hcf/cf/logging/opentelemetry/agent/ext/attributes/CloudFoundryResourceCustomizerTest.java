package com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.testing.ResourceAssertions.assertStringAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CloudFoundryResourceCustomizerTest {

    private static final Resource DEFAULT_CF_RESOURCE =
            Resource.builder().put("cloudfoundry.app.id", "test-app-id").put("cloudfoundry.app.instance.id", "42")
                    .put("cloudfoundry.app.name", "test-application").put("cloudfoundry.org.id", "test-org-id")
                    .put("cloudfoundry.org.name", "test-org").put("cloudfoundry.process.id", "test-process-id")
                    .put("cloudfoundry.process.type", "test-process-type").put("cloudfoundry.space.id", "test-space-id")
                    .put("cloudfoundry.space.name", "test-space").build();

    @Test
    void emptyResourceWithNullResource() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource = customizer.apply(null, DefaultConfigProperties.createFromMap(new HashMap<>()));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    void emptyResourceWhenNotInCf() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource =
                customizer.apply(Resource.builder().build(), DefaultConfigProperties.createFromMap(new HashMap<>()));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    void emptyResourceWhenDisabledByProperty() {

        HashMap<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.resource.enabled", "false");

        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource =
                customizer.apply(Resource.builder().build(), DefaultConfigProperties.createFromMap(properties));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    void fillsResourceFromVcapApplication() {

        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource =
                customizer.apply(DEFAULT_CF_RESOURCE, DefaultConfigProperties.createFromMap(Collections.emptyMap()));
        assertStringAttribute(resource, "service.name").isEqualTo("test-application");
        assertStringAttribute(resource, "sap.cf.app_name").isEqualTo("test-application");
        assertStringAttribute(resource, "sap.cf.app_id").isEqualTo("test-app-id");
        assertStringAttribute(resource, "sap.cf.space_name").isEqualTo("test-space");
        assertStringAttribute(resource, "sap.cf.org_name").isEqualTo("test-org");
        assertStringAttribute(resource, "sap.cf.instance_id").isEqualTo("42");
        assertStringAttribute(resource, "sap.cf.process.id").isEqualTo("test-process-id");
        assertStringAttribute(resource, "sap.cf.process.type").isEqualTo("test-process-type");
        assertStringAttribute(resource, "sap.cf.source_id").isEqualTo("test-app-id");
    }

    @Test
    void keepsOriginalResourceOnOTelResourceFormat() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        HashMap<String, String> config = new HashMap<String, String>() {{
            put("otel.javaagent.extension.sap.cf.resource.format", "opentelemetry");
        }};
        Resource resource = customizer.apply(DEFAULT_CF_RESOURCE, DefaultConfigProperties.createFromMap(config));

        assertEquals(DEFAULT_CF_RESOURCE, resource);
    }
}
