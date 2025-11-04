package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.List;
import java.util.function.Supplier;

public class DynatraceServiceProvider implements Supplier<CloudFoundryServiceInstance> {

    private static final String DEFAULT_USER_PROVIDED_LABEL = "user-provided";
    private static final String DEFAULT_DYNATRACE_LABEL = "dynatrace";
    private static final String DEFAULT_DYNATRACE_TAG = "dynatrace";

    private final CloudFoundryServiceInstance service;

    public DynatraceServiceProvider(ConfigProperties config) {
        this(config, new CloudFoundryServicesAdapter());
    }

    DynatraceServiceProvider(ConfigProperties config, CloudFoundryServicesAdapter adapter) {
        List<String> serviceLabels = List.of(getUserProvidedLabel(config), getDynatraceLabel(config));
        List<String> serviceTags = List.of(getDynatraceTag(config));
        this.service = adapter.stream(serviceLabels, serviceTags).findFirst().orElse(null);
    }

    private String getUserProvidedLabel(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.user-provided.label",
                                DEFAULT_USER_PROVIDED_LABEL);
    }

    private String getDynatraceLabel(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.label", DEFAULT_DYNATRACE_LABEL);
    }

    private String getDynatraceTag(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.tag", DEFAULT_DYNATRACE_TAG);
    }

    @Override
    public CloudFoundryServiceInstance get() {
        return service;
    }
}
