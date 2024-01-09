package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudLoggingServicesProvider implements Supplier<Stream<CfService>> {

    private static final String DEFAULT_USER_PROVIDED_LABEL = "user-provided";
    private static final String DEFAULT_CLOUD_LOGGING_LABEL = "cloud-logging";
    private static final String DEFAULT_CLOUD_LOGGING_TAG = "Cloud Logging";

    private final List<CfService> services;

    public CloudLoggingServicesProvider(ConfigProperties config, CfEnv cfEnv) {
        String userProvidedLabel = getUserProvidedLabel(config);
        String cloudLoggingLabel = getCloudLoggingLabel(config);
        String cloudLoggingTag = getCloudLoggingTag(config);
        List<CfService> userProvided = cfEnv.findServicesByLabel(userProvidedLabel);
        List<CfService> managed = cfEnv.findServicesByLabel(cloudLoggingLabel);
        this.services = Stream.concat(userProvided.stream(), managed.stream())
                .filter(svc -> svc.existsByTagIgnoreCase(cloudLoggingTag))
                .collect(Collectors.toList());
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
