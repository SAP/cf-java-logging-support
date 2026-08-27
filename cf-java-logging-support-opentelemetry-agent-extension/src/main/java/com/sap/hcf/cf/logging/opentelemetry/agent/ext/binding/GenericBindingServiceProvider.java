package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Package-private provider that resolves a single {@link CloudFoundryServiceInstance} from {@code VCAP_SERVICES} using
 * a selector string. An instance matches if the selector equals its name, its service label, or any of its tags.
 */
class GenericBindingServiceProvider implements Supplier<Optional<CloudFoundryServiceInstance>> {

    private final CloudFoundryServicesAdapter adapter;
    private final String selector;

    GenericBindingServiceProvider(CloudFoundryServicesAdapter adapter, String selector) {
        this.adapter = adapter;
        this.selector = selector;
    }

    /**
     * Returns the first {@link CloudFoundryServiceInstance} whose name, label, or tag matches the selector, or
     * {@link Optional#empty()} when no match is found.
     */
    @Override
    public Optional<CloudFoundryServiceInstance> get() {
        return adapter.stream(Collections.emptyList(), Collections.emptyList())
                      .filter(this::matches)
                      .findFirst();
    }

    private boolean matches(CloudFoundryServiceInstance instance) {
        if (selector.equals(instance.getName())) return true;
        if (selector.equals(instance.getLabel())) return true;
        List<String> tags = instance.getTags();
        return tags != null && tags.contains(selector);
    }
}
