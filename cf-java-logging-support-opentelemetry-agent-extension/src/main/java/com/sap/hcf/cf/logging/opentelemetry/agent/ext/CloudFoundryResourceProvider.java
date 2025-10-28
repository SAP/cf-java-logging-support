package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.attributes.CloudFoundryResourceCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

import java.util.ServiceLoader;

public class CloudFoundryResourceProvider implements ResourceProvider {

    private final CloudFoundryResourceCustomizer customizer = new CloudFoundryResourceCustomizer();

    @Override
    public Resource createResource(ConfigProperties configProperties) {
        ResourceProvider delegate = getDelegate();
        return delegate == null
                ? Resource.empty()
                : customizer.apply(delegate.createResource(configProperties), configProperties);
    }

    private ResourceProvider getDelegate() {
        return DelegateHolder.INSTANCE;
    }

    private static class DelegateHolder {
        static final ResourceProvider INSTANCE = loadCloudFoundryResourceProvider();

        private static ResourceProvider loadCloudFoundryResourceProvider() {
            ServiceLoader<ResourceProvider> loader = ServiceLoader.load(ResourceProvider.class);
            return loader.stream().map(ServiceLoader.Provider::get)
                         .filter(p -> p instanceof io.opentelemetry.contrib.cloudfoundry.resources.CloudFoundryResourceProvider)
                         .findAny().orElse(null);
        }

    }
}
