package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.DEPRECATED;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.List;
import java.util.function.Supplier;

public class DynatraceServiceProvider implements Supplier<CloudFoundryServiceInstance> {

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
        return DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.USER_PROVIDED.LABEL_OTEL.getValue(config);
    }

    private String getDynatraceLabel(ConfigProperties config) {
        return RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.LABEL.getValue(config);
    }

    private String getDynatraceTag(ConfigProperties config) {
        return RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TAG.getValue(config);
    }

    @Override
    public CloudFoundryServiceInstance get() {
        return service;
    }
}
