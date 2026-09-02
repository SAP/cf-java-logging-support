package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class VcapServiceProvider implements Supplier<CloudFoundryServiceInstance> {

    private final CloudFoundryServiceInstance service;

    public VcapServiceProvider(ConfigProperties config) {
        this(config, CloudFoundryServicesAdapter.builder().build());
    }

    VcapServiceProvider(ConfigProperties config, CloudFoundryServicesAdapter adapter) {
        String label = RUNTIME.CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.LABEL.getValue(config);
        String tag = RUNTIME.CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.TAG.getValue(config);
        String name = RUNTIME.CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.NAME.getValue(config);

        List<String> serviceLabels = label != null ? List.of(label) : Collections.emptyList();
        List<String> serviceTags = tag != null ? List.of(tag) : Collections.emptyList();

        this.service = adapter.stream(serviceLabels, serviceTags, name).findFirst().orElse(null);
    }

    @Override
    public CloudFoundryServiceInstance get() {
        return service;
    }
}
