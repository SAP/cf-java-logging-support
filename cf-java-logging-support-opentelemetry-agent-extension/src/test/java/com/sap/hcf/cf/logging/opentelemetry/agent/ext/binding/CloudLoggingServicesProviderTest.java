package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingServicesProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class CloudLoggingServicesProviderTest {

    private static final String DEFAULT_VCAP_APPLICATION = "{}";
    private static final String DEFAULT_VCAP_SERVICES = "{" +
            "\"cloud-logging\":[" +
            "{\"label\":\"cloud-logging\", \"tags\":[\"Cloud Logging\"],\"name\":\"managed-service1\"}," +
            "{\"label\":\"cloud-logging\", \"tags\":[\"Cloud Logging\"],\"name\":\"managed-service2\"}" +
            "]," +
            "\"not-cloud-logging\":[" +
            "{\"label\":\"not-cloud-logging\", \"tags\":[\"Cloud Logging\"],\"name\":\"managed-other\"}" +
            "]," +
            "\"user-provided\":[" +
            "{\"label\":\"cloud-logging\", \"tags\":[\"Cloud Logging\"],\"name\":\"ups-cloud-logging\"}," +
            "{\"label\":\"cloud-logging\", \"tags\":[\"NOT Cloud Logging\"],\"name\":\"ups-other\"}" +
            "]}";

    @Test
    public void defaultLabelsAndTags() {
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(Collections.emptyMap());
        CfEnv cfEnv = new CfEnv(DEFAULT_VCAP_APPLICATION, DEFAULT_VCAP_SERVICES);
        CloudLoggingServicesProvider servicesProvider = new CloudLoggingServicesProvider(emptyProperties, cfEnv);
        List<String> serviceNames = servicesProvider.get().map(CfService::getName).collect(Collectors.toList());
        assertThat(serviceNames, hasSize(3));
        assertThat(serviceNames, hasItem("managed-service1"));
        assertThat(serviceNames, hasItem("managed-service2"));
        assertThat(serviceNames, hasItem("ups-cloud-logging"));
    }

    @Test
    public void customLabel() {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.binding.cloud-logging.label", "not-cloud-logging");
        properties.put("otel.javaagent.extension.sap.cf.binding.user-provided.label", "unknown-label");
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(properties);
        CfEnv cfEnv = new CfEnv(DEFAULT_VCAP_APPLICATION, DEFAULT_VCAP_SERVICES);
        CloudLoggingServicesProvider servicesProvider = new CloudLoggingServicesProvider(emptyProperties, cfEnv);
        List<String> serviceNames = servicesProvider.get().map(CfService::getName).collect(Collectors.toList());
        assertThat(serviceNames, hasSize(1));
        assertThat(serviceNames, hasItem("managed-other"));
    }

    @Test
    public void customTag() {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.javaagent.extension.sap.cf.binding.cloud-logging.tag", "NOT Cloud Logging");
        DefaultConfigProperties emptyProperties = DefaultConfigProperties.createFromMap(properties);
        CfEnv cfEnv = new CfEnv(DEFAULT_VCAP_APPLICATION, DEFAULT_VCAP_SERVICES);
        CloudLoggingServicesProvider servicesProvider = new CloudLoggingServicesProvider(emptyProperties, cfEnv);
        List<String> serviceNames = servicesProvider.get().map(CfService::getName).collect(Collectors.toList());
        assertThat(serviceNames, hasSize(1));
        assertThat(serviceNames, hasItem("ups-other"));
    }

}