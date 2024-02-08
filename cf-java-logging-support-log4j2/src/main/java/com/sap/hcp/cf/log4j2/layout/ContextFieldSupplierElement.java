package com.sap.hcp.cf.log4j2.layout;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

@Plugin(name = "contextFieldSupplier", category = Node.CATEGORY, printObject = true)
public class ContextFieldSupplierElement {

    private String supplierClass;

    public ContextFieldSupplierElement(Builder builder) {
        this.supplierClass = builder.clazz;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public String getSupplierClass() {
        return supplierClass;
    }

    @Override
    public String toString() {
        return supplierClass;
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ContextFieldSupplierElement> {

        @PluginBuilderAttribute("class")
        private String clazz;

        public Builder setClazz(String clazz) {
            this.clazz = clazz;
            return this;
        }

        @Override
        public ContextFieldSupplierElement build() {
            return new ContextFieldSupplierElement(this);
        }

    }

}
