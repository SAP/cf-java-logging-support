package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes.CloudFoundryResourceCustomizer;
import io.opentelemetry.contrib.cloudfoundry.resources.CloudFoundryResource;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

public class CloudFoundryResourceProvider implements ResourceProvider {

    private final CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();

    @Override
    public Resource createResource(ConfigProperties configProperties) {
        Resource original = CloudFoundryResource.get();
        return customizer.apply(original, configProperties);
    }
}
