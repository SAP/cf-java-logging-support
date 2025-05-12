package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes.CloudFoundryResourceCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

public class CloudFoundryResourceProvider
        extends io.opentelemetry.contrib.cloudfoundry.resources.CloudFoundryResourceProvider {

    private final CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();

    @Override
    public Resource createResource(ConfigProperties configProperties) {
        Resource original = super.createResource(configProperties);
        return customizer.apply(original, configProperties);
    }
}
