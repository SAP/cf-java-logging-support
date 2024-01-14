package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class CloudLoggingServicesProvider implements Supplier<Stream<CfService>> {

    private static final String DEFAULT_USER_PROVIDED_LABEL = "user-provided";
    private static final String DEFAULT_CLOUD_LOGGING_LABEL = "cloud-logging";
    private static final String DEFAULT_CLOUD_LOGGING_TAG = "Cloud Logging";

    private final List<CfService> services;

    public CloudLoggingServicesProvider(ConfigProperties config) {
        this(config, new CloudFoundryServicesAdapter(new CfEnv()));
    }

    CloudLoggingServicesProvider(ConfigProperties config, CloudFoundryServicesAdapter adapter) {
        List<String> serviceLabels = asList(getUserProvidedLabel(config), getCloudLoggingLabel(config));
        List<String> serviceTags = singletonList(getCloudLoggingTag(config));
        this.services = adapter.stream(serviceLabels, serviceTags).collect(toList());
    }

    private String getUserProvidedLabel(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.user-provided.label", DEFAULT_USER_PROVIDED_LABEL);
    }

    private String getCloudLoggingLabel(ConfigProperties config) {
        String fromOwnProperties = System.getProperty("com.sap.otel.extension.cloud-logging.label", DEFAULT_CLOUD_LOGGING_LABEL);
        return config.getString("otel.javaagent.extension.sap.cf.binding.cloud-logging.label", fromOwnProperties);
    }

    private String getCloudLoggingTag(ConfigProperties config) {
        String fromOwnProperties = System.getProperty("com.sap.otel.extension.cloud-logging.tag", DEFAULT_CLOUD_LOGGING_TAG);
        return config.getString("otel.javaagent.extension.sap.cf.binding.cloud-logging.tag", fromOwnProperties);
    }

    @Override
    public Stream<CfService> get() {
        return services.stream();
    }
}
