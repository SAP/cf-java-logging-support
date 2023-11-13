package com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.pivotal.cfenv.core.CfApplication;
import io.pivotal.cfenv.core.CfEnv;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class CloudFoundryResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {

    private static final String OTEL_JAVAAGENT_EXTENSION_SAP_CF_RESOURCE_ENABLED = "otel.javaagent.extension.sap.cf.resource.enabled";
    private static final Logger LOG = Logger.getLogger(CloudFoundryResourceCustomizer.class.getName());
    private final CfEnv cfEnv;

    public CloudFoundryResourceCustomizer(CfEnv cfEnv) {
        this.cfEnv = cfEnv;
    }

    @Override
    public Resource apply(Resource resource, ConfigProperties configProperties) {
        boolean isEnabled = configProperties.getBoolean(OTEL_JAVAAGENT_EXTENSION_SAP_CF_RESOURCE_ENABLED, true);
        if (!isEnabled) {
            LOG.config("CF resource attributes are disabled by configuration.");
            return Resource.empty();
        }
        if (!cfEnv.isInCf()) {
            LOG.config("Not running in CF. Cannot obtain CF resource attributes.");
            return Resource.empty();
        }
        CfApplication cfApp = cfEnv.getApp();
        ResourceBuilder rb = Resource.builder();
        rb.put("service.name", cfApp.getApplicationName());
        rb.put("sap.cf.source_id", getString(cfApp, "source_id"));
        rb.put("sap.cf.instance_id", cfApp.getInstanceIndex());
        rb.put("sap.cf.app_id", cfApp.getApplicationId());
        rb.put("sap.cf.app_name", cfApp.getApplicationName());
        rb.put("sap.cf.space_id", cfApp.getSpaceId());
        rb.put("sap.cf.space_name", cfApp.getSpaceName());
        rb.put("sap.cf.org_id", getString(cfApp, "organization_id"));
        rb.put("sap.cf.org_name", getString(cfApp, "organization_name"));
        return rb.build();
    }

    private String getString(CfApplication cfApp, String key) {
        return Optional.ofNullable(cfApp)
                .map(CfApplication::getMap)
                .map(m -> m.get(key))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse("");
    }
}
