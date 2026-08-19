package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpanAttributeNameFilterCustomizerTest {

    private static final Map<String, String> INCLUSIONS =
            Map.of("sap.cf.integration.otel.extension.sanitizer.span.attribute.filter.include.names",
                   "included,prefix*");
    private static final Map<String, String> EXCLUSIONS =
            Map.of("sap.cf.integration.otel.extension.sanitizer.span.attribute.filter.exclude.names",
                   "excluded,prefix*");
    private static final Map<String, String> DISABLED =
            Map.of("sap.cf.integration.otel.extension.sanitizer.enabled", "false");
    private static final Attributes ATTRIBUTES =
            Attributes.builder().put("included", "included-value").put("excluded", "excluded-value")
                      .put("other", "other-value").put("prefix-test", "prefix-value").build();

    @SafeVarargs
    private static ConfigProperties configOf(Map<String, String>... partialConfigs) {
        if (partialConfigs == null) {
            return DefaultConfigProperties.createFromMap(Collections.emptyMap());
        }
        Map<String, String> merged = new HashMap<>();
        for (Map<String, String> current: partialConfigs) {
            merged.putAll(current);
        }
        return DefaultConfigProperties.createFromMap(merged);
    }

    @Test
    void isDisabledByDefault() {
        SpanAttributeNameFilterCustomizer customizer = new SpanAttributeNameFilterCustomizer();
        assertFalse(customizer.isEnabled(null));
    }

    @Test
    void isEnabledWhenConfigured() {
        assertTrue(new SpanAttributeNameFilterCustomizer().isEnabled(configOf(INCLUSIONS)));
        assertTrue(new SpanAttributeNameFilterCustomizer().isEnabled(configOf(EXCLUSIONS)));
    }

    @Test
    void isDisabledWhenSanitizerIsDisabled() {
        assertFalse(new SpanAttributeNameFilterCustomizer().isEnabled(configOf(INCLUSIONS, DISABLED)));
    }

    @Test
    void isApplicableOnlyWhenConfiguredAttributeNamesLeadToDrop() {
        SpanAttributeNameFilterCustomizer customizer = new SpanAttributeNameFilterCustomizer();
        customizer.isEnabled(configOf(INCLUSIONS, EXCLUSIONS));
        assertFalse(customizer.isApplicable(Attributes.builder().put("included", "ignored").build()),
                    "included key does not require changes to attributes");
        assertTrue(customizer.isApplicable(Attributes.builder().put("excluded", "ignored").build()),
                   "excluded key requires changes to attributes");
        assertTrue(customizer.isApplicable(Attributes.builder().put("prefix-test", "ignored").build()),
                   "excluded prefix requires changes to attributes");
        assertTrue(customizer.isApplicable(Attributes.builder().put("other", "other-value").build()),
                   "not included key requires changes to attributes");
    }

    @Test
    void allowsAllAttributesWithoutConfig() {
        SpanAttributeNameFilterCustomizer customizer = new SpanAttributeNameFilterCustomizer();
        customizer.isEnabled(configOf());
        AttributesBuilder result = ATTRIBUTES.toBuilder();
        customizer.customize(result, ATTRIBUTES);
        assertAttributeStringKey(result, "included").isEqualTo("included-value");
        assertAttributeStringKey(result, "excluded").isEqualTo("excluded-value");
        assertAttributeStringKey(result, "other").isEqualTo("other-value");
        assertAttributeStringKey(result, "prefix-test").isEqualTo("prefix-value");
    }

    private static AbstractObjectAssert<?, String> assertAttributeStringKey(AttributesBuilder result, String name) {
        return assertThat(result.build()).extracting(a -> a.get(AttributeKey.stringKey(name)));
    }

    @Test
    void allowsOnlyIncludedAttributes() {
        SpanAttributeNameFilterCustomizer customizer = new SpanAttributeNameFilterCustomizer();
        customizer.isEnabled(configOf(INCLUSIONS));
        AttributesBuilder result = ATTRIBUTES.toBuilder();
        customizer.customize(result, ATTRIBUTES);
        assertAttributeStringKey(result, "included").isEqualTo("included-value");
        assertAttributeStringKey(result, "excluded").isNull();
        assertAttributeStringKey(result, "other").isNull();
        assertAttributeStringKey(result, "prefix-test").isEqualTo("prefix-value");
    }

    @Test
    void removesExcludedAttributes() {
        SpanAttributeNameFilterCustomizer customizer = new SpanAttributeNameFilterCustomizer();
        customizer.isEnabled(configOf(EXCLUSIONS));
        AttributesBuilder result = ATTRIBUTES.toBuilder();
        customizer.customize(result, ATTRIBUTES);
        assertAttributeStringKey(result, "included").isEqualTo("included-value");
        assertAttributeStringKey(result, "excluded").isNull();
        assertAttributeStringKey(result, "other").isEqualTo("other-value");
        assertAttributeStringKey(result, "prefix-test").isNull();
    }

    @Test
    void exclusionsTakePriorityOverInclusions() {
        SpanAttributeNameFilterCustomizer customizer = new SpanAttributeNameFilterCustomizer();
        customizer.isEnabled(configOf(INCLUSIONS, EXCLUSIONS));
        AttributesBuilder result = ATTRIBUTES.toBuilder();
        customizer.customize(result, ATTRIBUTES);
        assertAttributeStringKey(result, "included").isEqualTo("included-value");
        assertAttributeStringKey(result, "excluded").isNull();
        assertAttributeStringKey(result, "other").isNull();
        assertAttributeStringKey(result, "prefix-test").isNull();
    }
}
