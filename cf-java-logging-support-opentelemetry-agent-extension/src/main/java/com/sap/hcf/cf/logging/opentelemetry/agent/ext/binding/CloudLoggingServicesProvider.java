package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.DEPRECATED;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class CloudLoggingServicesProvider implements Supplier<Stream<CloudFoundryServiceInstance>> {

    private final List<CloudFoundryServiceInstance> services;

    public CloudLoggingServicesProvider(ConfigProperties config) {
        this(config, new CloudFoundryServicesAdapter());
    }

    CloudLoggingServicesProvider(ConfigProperties config, CloudFoundryServicesAdapter adapter) {
        List<String> serviceLabels = List.of(getUserProvidedLabel(config), getCloudLoggingLabel(config));
        List<String> serviceTags = List.of(getCloudLoggingTag(config));
        this.services = adapter.stream(serviceLabels, serviceTags).collect(toList());
    }

    private String getUserProvidedLabel(ConfigProperties config) {
        return DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.USER_PROVIDED.LABEL_OTEL.getValue(config);
    }

    private String getCloudLoggingLabel(ConfigProperties config) {
        return RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.LABEL.getValue(config);
    }

    private String getCloudLoggingTag(ConfigProperties config) {
        return RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.TAG.getValue(config);
    }

    @Override
    public Stream<CloudFoundryServiceInstance> get() {
        return services.stream();
    }
}
