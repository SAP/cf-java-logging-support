package com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class CloudFoundryResourceCustomizer implements BiFunction<Resource, ConfigProperties, Resource> {

    private static final String OTEL_JAVAAGENT_EXTENSION_SAP_CF_RESOURCE_ENABLED =
            "otel.javaagent.extension.sap.cf.resource.enabled";
    private static final String OTEL_JAVAAGENT_EXTENTION_CF_RESOURCE_FORMAT =
            "otel.javaagent.extension.sap.cf.resource.format";
    private static final Logger LOG = Logger.getLogger(CloudFoundryResourceCustomizer.class.getName());
    private static final Map<String, String> SAP_CF_RESOURCE_ATTRIBUTES = new HashMap<String, String>() {{
        put("cloudfoundry.app.id", "sap.cf.app_id");
        put("cloudfoundry.app.instance.id", "sap.cf.instance_id");
        put("cloudfoundry.app.name", "sap.cf.app_name");
        put("cloudfoundry.org.id", "sap.cf.org_id");
        put("cloudfoundry.org.name", "sap.cf.org_name");
        put("cloudfoundry.process.id", "sap.cf.process.id");
        put("cloudfoundry.process.type", "sap.cf.process.type");
        put("cloudfoundry.space.id", "sap.cf.space_id");
        put("cloudfoundry.space.name", "sap.cf.space_name");
    }};

    @Override
    public Resource apply(Resource resource, ConfigProperties configProperties) {
        boolean isEnabled = configProperties.getBoolean(OTEL_JAVAAGENT_EXTENSION_SAP_CF_RESOURCE_ENABLED, true);
        if (!isEnabled) {
            LOG.config("CF resource attributes are disabled by configuration.");
            return Resource.empty();
        }

        if (resource == null) {
            LOG.config("Not running in CF. Cannot obtain CF resource.");
            return Resource.empty();
        }

        if (resource.getAttributes().isEmpty()) {
            LOG.config("Not running in CF. Cannot obtain CF resource attributes.");
            return resource;
        }

        String format = configProperties.getString(OTEL_JAVAAGENT_EXTENTION_CF_RESOURCE_FORMAT, "SAP");
        if (!format.equalsIgnoreCase("SAP")) {
            return resource;
        }

        ResourceBuilder builder = Resource.builder();
        resource.getAttributes().asMap().forEach(addAttribute(builder));
        String appId = resource.getAttribute(AttributeKey.stringKey("cloudfoundry.app.id"));
        builder.put("sap.cf.source_id", appId);
        String appName = resource.getAttribute(AttributeKey.stringKey("cloudfoundry.app.name"));
        String serviceName = resource.getAttribute(AttributeKey.stringKey("service.name"));
        if (serviceName == null) {
            builder.put("service.name", appName);
        }
        return builder.build();
    }

    private BiConsumer<AttributeKey<?>, Object> addAttribute(ResourceBuilder builder) {
        return (k, v) -> {
            switch (k.getType()) {
            case BOOLEAN:
                builder.put(rename(k), (boolean) v);
                break;
            case BOOLEAN_ARRAY:
                builder.put(rename(k), (boolean[]) v);
                break;
            case DOUBLE:
                builder.put(rename(k), (double) v);
                break;
            case DOUBLE_ARRAY:
                builder.put(rename(k), (double[]) v);
                break;
            case LONG:
                builder.put(rename(k), (long) v);
                break;
            case LONG_ARRAY:
                builder.put(rename(k), (long[]) v);
                break;
            case STRING:
                builder.put(rename(k), (String) v);
                break;
            case STRING_ARRAY:
                builder.put(rename(k), (String[]) v);
                break;
            }
        };
    }

    private String rename(AttributeKey<?> key) {
        String name = key.getKey();
        return SAP_CF_RESOURCE_ATTRIBUTES.getOrDefault(name, name);
    }
}
