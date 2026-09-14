package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.Collections;
import java.util.function.Supplier;

/**
 * Provides the CaaS (Collector as a Service) service binding.
 *
 * @deprecated CaaS is an SAP internal only service. The service bindings supported by this extension (v1) are no longer created. Will be removed in a future release.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class CaasServiceProvider implements Supplier<CloudFoundryServiceInstance> {

    private final CloudFoundryServiceInstance service;

    public CaasServiceProvider(ConfigProperties config) {
        this(config, CloudFoundryServicesAdapter.builder().build());
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
