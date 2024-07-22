package com.sap.hcp.cf.log4j2.layout;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

@Plugin(name = "customField", category = Node.CATEGORY, printObject = true)
public class CustomFieldElement {

    private final String key;
    private final boolean retainOriginal;

    private CustomFieldElement(Builder builder) {
        this.key = builder.key;
        this.retainOriginal = builder.retainOriginal;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public String getKey() {
        return key;
    }

    public boolean isRetainOriginal() {
        return retainOriginal;
    }

    @Override
    public String toString() {
        return key + (retainOriginal ? " (retained)" : "");
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<CustomFieldElement> {

        @PluginBuilderAttribute("mdcKeyName")
        private String key;

        @PluginBuilderAttribute("retainOriginal")
        private boolean retainOriginal;

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setRetainOriginal(boolean retainOriginal) {
            this.retainOriginal = retainOriginal;
            return this;
        }

        @Override
        public CustomFieldElement build() {
            return new CustomFieldElement(this);
        }

    }
}
