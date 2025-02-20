package com.sap.hcp.cf.log4j2.layout;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

@Plugin(name = "log4jContextFieldSupplier", category = Node.CATEGORY, printObject = true)
public class Log4jContextFieldSupplierElement implements ElementSupplier {

    private final String supplierClass;

    public Log4jContextFieldSupplierElement(Builder builder) {
        this.supplierClass = builder.clazz;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String getSupplierClass() {
        return supplierClass;
    }

    @Override
    public String toString() {
        return supplierClass;
    }

    public static class Builder
            implements org.apache.logging.log4j.core.util.Builder<Log4jContextFieldSupplierElement> {

        @PluginBuilderAttribute("class")
        private String clazz;

        public Builder setClazz(String clazz) {
            this.clazz = clazz;
            return this;
        }

        @Override
        public Log4jContextFieldSupplierElement build() {
            return new Log4jContextFieldSupplierElement(this);
        }

    }

}
