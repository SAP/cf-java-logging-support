package com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CloudFoundryResourceCustomizerTest {

    private static final Resource DEFAULT_CF_RESOURCE =
            Resource.builder().put("cloudfoundry.app.id", "test-app-id").put("cloudfoundry.app.instance.id", "42")
                    .put("cloudfoundry.app.name", "test-application").put("cloudfoundry.org.id", "test-org-id")
                    .put("cloudfoundry.org.name", "test-org").put("cloudfoundry.process.id", "test-process-id")
                    .put("cloudfoundry.process.type", "test-process-type").put("cloudfoundry.space.id", "test-space-id")
                    .put("cloudfoundry.space.name", "test-space").build();

    @Test
    public void emptyResourceWhenNotInCf() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource =
                customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(new HashMap<>()));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    public void emptyResourceWhenDisabledByProperty() {

        HashMap<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.resource.enabled", "false");

        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource = customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(properties));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    public void fillsResourceFromVcapApplication() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        Resource resource =
                customizer.apply(DEFAULT_CF_RESOURCE, DefaultConfigProperties.create(Collections.emptyMap()));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("sap.cf.app_name")));
        assertEquals("test-app-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.app_id")));
        assertEquals("test-space", resource.getAttribute(AttributeKey.stringKey("sap.cf.space_name")));
        assertEquals("test-org", resource.getAttribute(AttributeKey.stringKey("sap.cf.org_name")));
        assertEquals("42", resource.getAttribute(AttributeKey.stringKey("sap.cf.instance_id")));
        assertEquals("test-process-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.process.id")));
        assertEquals("test-process-type", resource.getAttribute(AttributeKey.stringKey("sap.cf.process.type")));
        assertEquals("test-app-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.source_id")));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("service.name")));
    }

    @Test
    public void keepsOriginalResourceOnOTelResourceFormat() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();
        HashMap<String, String> config = new HashMap<String, String>() {{
            put("otel.javaagent.extension.sap.cf.resource.format", "opentelemetry");
        }};
        Resource resource = customizer.apply(DEFAULT_CF_RESOURCE, DefaultConfigProperties.create(config));

        assertEquals(DEFAULT_CF_RESOURCE, resource);
    }
}
