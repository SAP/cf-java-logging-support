package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

class DynatraceServiceProvider implements Supplier<CfService> {

    private static final String DEFAULT_USER_PROVIDED_LABEL = "user-provided";
    private static final String DEFAULT_DYNATRACE_LABEL = "dynatrace";
    private static final String DEFAULT_DYNATRACE_TAG = "dynatrace";

    private final CfService service;

    public DynatraceServiceProvider(ConfigProperties config, CfEnv cfEnv) {
        String userProvidedLabel = getUserProvidedLabel(config);
        String dynatraceLabel = getDynatraceLabel(config);
        String dynatraceTag = getDynatraceTag(config);
        List<CfService> userProvided = cfEnv.findServicesByLabel(userProvidedLabel);
        List<CfService> managed = cfEnv.findServicesByLabel(dynatraceLabel);
        this.service = Stream.concat(userProvided.stream(), managed.stream())
                .filter(svc -> svc.existsByTagIgnoreCase(dynatraceTag)).findFirst().orElse(null);

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
