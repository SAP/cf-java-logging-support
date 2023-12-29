package com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.pivotal.cfenv.core.CfApplication;
import io.pivotal.cfenv.core.CfEnv;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CloudFoundryResourceCustomizerTest {

    @Test
    public void emptyResourceWhenNotInCf() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer(new CfEnv());
        Resource resource = customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(new HashMap<>()));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    public void emptyResourceWhenDisabledByProperty() {
        CfEnv cfEnv = Mockito.mock(CfEnv.class);
        when(cfEnv.isInCf()).thenReturn(true);

        HashMap<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.resource.enabled", "false");

        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer(cfEnv);
        Resource resource = customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(properties));
        assertTrue(resource.getAttributes().isEmpty());
    }

    @Test
    public void fillsResourceFromVcapApplication() {
        CfEnv cfEnv = Mockito.mock(CfEnv.class);
        when(cfEnv.isInCf()).thenReturn(true);
        Map<String, Object> applicationData = new HashMap<>();
        applicationData.put("application_name", "test-application");
        applicationData.put("space_name", "test-space");
        applicationData.put("organization_name", "test-org");
        applicationData.put("application_id", "test-app-id");
        applicationData.put("instance_index", 42);
        applicationData.put("process_id", "test-process-id");
        applicationData.put("process_type", "test-process-type");
        when(cfEnv.getApp()).thenReturn(new CfApplication(applicationData));

        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer(cfEnv);
        Resource resource = customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(new HashMap<>()));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("sap.cf.app_name")));
        assertEquals("test-space", resource.getAttribute(AttributeKey.stringKey("sap.cf.space_name")));
        assertEquals("test-org", resource.getAttribute(AttributeKey.stringKey("sap.cf.org_name")));
        assertEquals("test-app-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.source_id")));
        assertEquals(42, resource.getAttribute(AttributeKey.longKey("sap.cf.instance_id")).longValue());
        assertEquals("test-process-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.process.id")));
        assertEquals("test-process-type", resource.getAttribute(AttributeKey.stringKey("sap.cf.process.type")));
    }
}