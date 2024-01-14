package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class DynatraceServiceProvider implements Supplier<CfService> {

    private static final String DEFAULT_USER_PROVIDED_LABEL = "user-provided";
    private static final String DEFAULT_DYNATRACE_LABEL = "dynatrace";
    private static final String DEFAULT_DYNATRACE_TAG = "dynatrace";

    private final CfService service;

    public DynatraceServiceProvider(ConfigProperties config) {
        this(config, new CloudFoundryServicesAdapter(new CfEnv()));
    }

    DynatraceServiceProvider(ConfigProperties config, CloudFoundryServicesAdapter adapter) {
        List<String> serviceLabels = asList(getUserProvidedLabel(config), getDynatraceLabel(config));
        List<String> serviceTags = singletonList(getDynatraceTag(config));
        this.service = adapter.stream(serviceLabels, serviceTags).findFirst().orElse(null);
    }

    private String getUserProvidedLabel(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.user-provided.label", DEFAULT_USER_PROVIDED_LABEL);
    }

    private String getDynatraceLabel(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.label", DEFAULT_DYNATRACE_LABEL);
    }

    private String getDynatraceTag(ConfigProperties config) {
        return config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.tag", DEFAULT_DYNATRACE_TAG);
    }

    @Override
    public CfService get() {
        return service;
    }
}
