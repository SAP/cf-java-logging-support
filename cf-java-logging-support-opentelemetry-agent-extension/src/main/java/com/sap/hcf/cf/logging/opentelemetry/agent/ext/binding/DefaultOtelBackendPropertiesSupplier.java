package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;
import static java.util.function.Predicate.not;

public class DefaultOtelBackendPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final Logger LOG = Logger.getLogger(DefaultOtelBackendPropertiesSupplier.class.getName());

    private final List<Supplier<Map<String, String>>> suppliers;

    private DefaultOtelBackendPropertiesSupplier(Builder builder) {
        this.suppliers = builder.suppliers;
    }

    @Override
    public Map<String, String> get() {
        if (suppliers.isEmpty()) {
            LOG.config("No OpenTelemetry backend properties suppliers configured.");
            return emptyMap();
        }
        return suppliers.stream().map(Supplier::get).filter(not(Map::isEmpty)).findFirst().orElse(emptyMap());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<Supplier<Map<String, String>>> suppliers = new ArrayList<>();

        public Builder add(Supplier<Map<String, String>> supplier) {
            suppliers.add(supplier);
            return this;
        }

        public DefaultOtelBackendPropertiesSupplier build() {
            return new DefaultOtelBackendPropertiesSupplier(this);
        }
    }
}
