package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import java.util.ArrayList;
import java.util.List;

public class CloudFoundryServiceInstance {

    private final String name;
    private final String label;
    private final List<String> tags;
    private final CloudFoundryCredentials credentials;

    private CloudFoundryServiceInstance(String name, String label, CloudFoundryCredentials credentials,
                                        List<String> tags) {
        this.name = name;
        this.label = label;
        this.credentials = credentials;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public CloudFoundryCredentials getCredentials() {
        return credentials;
    }

    public List<String> getTags() {
        return tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;
        private String label;
        private final List<String> tags = new ArrayList<>();
        private CloudFoundryCredentials credentials;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder tag(String tag) {
            if (tag != null && !tag.trim().isEmpty()) {
                tags.add(tag);
            }
            return this;
        }

        public Builder credentials(CloudFoundryCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        public CloudFoundryServiceInstance build() {
            return new CloudFoundryServiceInstance(name, label, credentials, tags);
        }

    }
}
