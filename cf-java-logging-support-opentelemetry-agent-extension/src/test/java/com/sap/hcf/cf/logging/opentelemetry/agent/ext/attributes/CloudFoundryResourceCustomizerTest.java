package com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.pivotal.cfenv.core.CfApplication;
import io.pivotal.cfenv.core.CfEnv;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CloudFoundryResourceCustomizerTest {

    private static String getStringAttribute(Resource r, String key) {
        return r.getAttribute(AttributeKey.stringKey(key));
    }

    @Test
    public void emptyResourceWhenNotInCf() {
        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer(new CfEnv());
        Resource resource =
                customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(new HashMap<>()));
        assertThat(resource.getAttributes().isEmpty()).isTrue();
    }

    @Test
    public void emptyResourceWhenDisabledByProperty() {
        CfEnv cfEnv = Mockito.mock(CfEnv.class);
        when(cfEnv.isInCf()).thenReturn(true);

        HashMap<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.resource.enabled", "false");

        CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer(cfEnv);
        Resource resource = customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(properties));
        assertThat(resource.getAttributes().isEmpty()).isTrue();
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
        Resource resource =
                customizer.apply(Resource.builder().build(), DefaultConfigProperties.create(new HashMap<>()));
        assertThat(resource).extracting(r -> getStringAttribute(r, "service.name")).isEqualTo("test-application");
        assertThat(resource).extracting(r -> getStringAttribute(r, "sap.cf.app_name")).isEqualTo("test-application");
        assertThat(resource).extracting(r -> getStringAttribute(r, "sap.cf.space_name")).isEqualTo("test-space");
        assertThat(resource).extracting(r -> getStringAttribute(r, "sap.cf.org_name")).isEqualTo("test-org");
        assertThat(resource).extracting(r -> getStringAttribute(r, "sap.cf.source_id")).isEqualTo("test-app-id");
        assertThat(resource).extracting(r -> r.getAttribute(AttributeKey.longKey("sap.cf.instance_id")).longValue())
                            .isEqualTo(42L);
        assertThat(resource).extracting(r -> getStringAttribute(r, "sap.cf.process.id")).isEqualTo("test-process-id");
        assertThat(resource).extracting(r -> getStringAttribute(r, "sap.cf.process.type"))
                            .isEqualTo("test-process-type");
    }
}
