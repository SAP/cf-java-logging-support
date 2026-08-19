package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.EXTENSION.SANITIZER;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class SpanAttributeNameFilterCustomizer implements SpanAttributeCustomizer {

    private Predicate<AttributeKey<?>> rejected = k -> false;

    @Override
    public boolean isEnabled(ConfigProperties config) {
        List<String> included = SANITIZER.SPAN.ATTRIBUTE.FILTER.INCLUDE_NAMES.getValue(config);
        List<String> excluded = SANITIZER.SPAN.ATTRIBUTE.FILTER.EXCLUDE_NAMES.getValue(config);

        List<String> includedNames = getNames(included);
        List<String> includedPrefixes = getPrefixes(included);
        List<String> excludedNames = getNames(excluded);
        List<String> excludedPrefixes = getPrefixes(excluded);
        this.rejected = k -> {
            String name = k.getKey();
            boolean isIncluded = (includedNames.isEmpty() && includedPrefixes.isEmpty()) || includedNames.contains(
                    name) || includedPrefixes.stream().anyMatch(name::startsWith);
            boolean isExcluded = excludedNames.contains(name) || excludedPrefixes.stream().anyMatch(name::startsWith);
            return !isIncluded || isExcluded;
        };

        return SANITIZER.ENABLED.getValue(config) && (!included.isEmpty() || !excluded.isEmpty());
    }

    private static List<String> getPrefixes(List<String> included) {
        return included.stream().filter(s -> s.endsWith("*")).map(s -> s.substring(0, s.length() - 1))
                       .collect(Collectors.toList());
    }

    private static List<String> getNames(List<String> included) {
        return included.stream().filter(not(s -> s.endsWith("*"))).collect(Collectors.toList());
    }

    @Override
    public boolean isApplicable(Attributes attributes) {
        return attributes.asMap().keySet().stream().anyMatch(rejected);
    }

    @Override
    public void customize(AttributesBuilder attributesBuilder, Attributes original) {
        attributesBuilder.removeIf(rejected);
    }
}
