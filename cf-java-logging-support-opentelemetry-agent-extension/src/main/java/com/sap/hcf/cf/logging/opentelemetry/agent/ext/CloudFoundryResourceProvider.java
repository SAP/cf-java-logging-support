package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes.CloudFoundryResourceCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.pivotal.cfenv.core.CfEnv;

public class CloudFoundryResourceProvider implements ResourceProvider {

    private final CloudFoundryResourceCustomizer customizer;

    public CloudFoundryResourceProvider() {
        this(new CfEnv());
    }

    public CloudFoundryResourceProvider(CfEnv cfEnv) {
        this.customizer = new CloudFoundryResourceCustomizer(cfEnv);
    }

    @Override
    public Resource createResource(ConfigProperties configProperties) {
        return customizer.apply(null, configProperties);
    }
}
