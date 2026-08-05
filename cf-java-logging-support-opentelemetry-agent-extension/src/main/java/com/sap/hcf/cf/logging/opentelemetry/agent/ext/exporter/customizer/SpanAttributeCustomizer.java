package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

public interface SpanAttributeCustomizer {

    /**
     * Returns true if this customizer is enabled based on the given configuration.
     *
     * @param config
     *         the configuration properties
     * @return true if this customizer is enabled, false otherwise
     */
    default boolean isEnabled(ConfigProperties config) {
        return true;
    }

    /**
     * Returns true if this customizer is applicable to the given attributes. This avoids object allocation for
     * attributes that are not relevant to this customizer.
     *
     * @param attributes
     *         the attributes to check
     * @return true if this customizer is applicable, false otherwise
     */
    default boolean isApplicable(Attributes attributes) {
        return false;
    }

    /**
     * Customizes the given attributes builder based on the original attributes. This should be a no-op if the
     * customizer is not applicable to the given attributes.
     *
     * @param attributesBuilder
     *         the attributes builder to customize
     * @param original
     *         the original attributes
     */
    void customize(AttributesBuilder attributesBuilder, Attributes original);
}
