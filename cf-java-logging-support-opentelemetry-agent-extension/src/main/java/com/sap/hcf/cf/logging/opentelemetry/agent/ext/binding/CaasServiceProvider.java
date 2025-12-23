package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.Collections;
import java.util.function.Supplier;

public class CaasServiceProvider implements Supplier<CloudFoundryServiceInstance> {

    private final CloudFoundryServiceInstance service;

    public CaasServiceProvider(ConfigProperties config) {
        this(config, new CloudFoundryServicesAdapter());
    }

    CaasServiceProvider(ConfigProperties config, CloudFoundryServicesAdapter adapter) {
        String label = ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CAAS.LABEL.getValue(config);
        this.service =
                adapter.stream(Collections.singletonList(label), Collections.emptyList()).findFirst().orElse(null);
    }

    @Override
    public CloudFoundryServiceInstance get() {
        return service;
    }
}
