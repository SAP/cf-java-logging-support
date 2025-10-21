package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import java.util.Map;
import java.util.TreeMap;

public class CloudFoundryCredentials {

    private final Map<String, String> properties;

    private CloudFoundryCredentials(Builder builder) {
        this.properties = builder.properties;
    }

    public String getString(String key) {
        return properties.get(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<String, String> properties = new TreeMap<>();

        private Builder() {
        }

        public CloudFoundryCredentials build() {
            return new CloudFoundryCredentials(this);
        }

        public Builder add(String key, String value) {
            if (isNotBlank(key) && isNotBlank(value)) {
                properties.put(key, value);
            }
            return this;
        }

        private boolean isNotBlank(String string) {
            return string != null && !string.trim().isEmpty();
        }
    }
}
