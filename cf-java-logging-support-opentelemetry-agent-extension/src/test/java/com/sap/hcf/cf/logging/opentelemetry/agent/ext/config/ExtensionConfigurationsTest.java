package com.sap.hcf.cf.logging.opentelemetry.agent.ext.config;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

class ExtensionConfigurationsTest {

    private static Stream<Arguments> provideStringProperties() {
        return Stream.of(of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.LOGS.COMPRESSION,
                            "otel.exporter.cloud-logging.logs.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.LOGS.COMPRESSION,
                            "otel.exporter.cloud-logging.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.COMPRESSION,
                            "otel.exporter.cloud-logging.metrics.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.COMPRESSION,
                            "otel.exporter.cloud-logging.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.TRACES.COMPRESSION,
                            "otel.exporter.cloud-logging.traces.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.TRACES.COMPRESSION,
                            "otel.exporter.cloud-logging.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.GENERAL.COMPRESSION,
                            "otel.exporter.cloud-logging.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.DYNATRACE.METRICS.COMPRESSION,
                            "otel.exporter.dynatrace.metrics.compression", "none"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.DEFAULT_HISTOGRAM_AGGREGATION,
                            "otel.exporter.cloud-logging.metrics.default.histogram.aggregation",
                            "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM"),
                         of(ExtensionConfigurations.EXPORTER.DYNATRACE.METRICS.DEFAULT_HISTOGRAM_AGGREGATION,
                            "otel.exporter.dynatrace.metrics.default.histogram.aggregation",
                            "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.TEMPORALITY_PREFERENCE,
                            "otel.exporter.cloud-logging.metrics.temporality.preference", "lowmemory"),
                         of(ExtensionConfigurations.EXPORTER.DYNATRACE.METRICS.TEMPORALITY_PREFERENCE,
                            "otel.exporter.dynatrace.metrics.temporality.preference", "lowmemory"),
                         of(ExtensionConfigurations.RESOURCE.CLOUD_FOUNDRY.FORMAT,
                            "sap.cloudfoundry.otel.resources.format", "OTEL"),
                         of(ExtensionConfigurations.RESOURCE.CLOUD_FOUNDRY.FORMAT,
                            "otel.javaagent.extension.sap.cf.resource.format", "OTEL"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CAAS.LABEL,
                            "sap.caas.cf.binding.label.value", "caas"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.LABEL,
                            "sap.cloud-logging.cf.binding.label.value", "cls"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.LABEL,
                            "otel.javaagent.extension.sap.cf.binding.cloud-logging.label", "cls"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.LABEL,
                            "com.sap.otel.extension.cloud-logging.label", "cls"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.TAG,
                            "sap.cloud-logging.cf.binding.tag.value", "cls"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.TAG,
                            "otel.javaagent.extension.sap.cf.binding.cloud-logging.tag", "cls"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.TAG,
                            "com.sap.otel.extension.cloud-logging.tag", "cls"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.LABEL,
                            "sap.dynatrace.cf.binding.label.value", "dt"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.LABEL,
                            "otel.javaagent.extension.sap.cf.binding.dynatrace.label", "dt"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TAG,
                            "sap.dynatrace.cf.binding.tag.value", "dt"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TAG,
                            "otel.javaagent.extension.sap.cf.binding.dynatrace.tag", "dt"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TOKEN_NAME,
                            "sap.dynatrace.cf.binding.token.name", "api-token"),
                         of(ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TOKEN_NAME,
                            "otel.javaagent.extension.sap.cf.binding.dynatrace.metrics.token-name", "api-token"));
    }

    private static Stream<Arguments> provideBooleanProperties() {
        return Stream.of(of(ExtensionConfigurations.EXTENSION.SANITIZER.ENABLED,
                            "sap.cf.integration.otel.extension.sanitizer.enabled", "false"),
                         of(ExtensionConfigurations.RESOURCE.CLOUD_FOUNDRY.ENABLED,
                            "sap.cloudfoundry.otel.resources.enabled", "false"),
                         of(ExtensionConfigurations.RESOURCE.CLOUD_FOUNDRY.ENABLED,
                            "otel.javaagent.extension.sap.cf.resource.enabled", "false"));
    }

    private static Stream<Arguments> provideDurationProperties() {
        return Stream.of(of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.LOGS.TIMEOUT,
                            "otel.exporter.cloud-logging.logs.timeout", "100"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.LOGS.TIMEOUT,
                            "otel.exporter.cloud-logging.timeout", "100"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.TIMEOUT,
                            "otel.exporter.cloud-logging.metrics.timeout", "100"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.TIMEOUT,
                            "otel.exporter.cloud-logging.timeout", "100"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.TRACES.TIMEOUT,
                            "otel.exporter.cloud-logging.traces.timeout", "100"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.TRACES.TIMEOUT,
                            "otel.exporter.cloud-logging.timeout", "100"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.GENERAL.TIMEOUT,
                            "otel.exporter.cloud-logging.timeout", "100"));
    }

    private static Stream<Arguments> provideListProperties() {
        return Stream.of(of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.EXCLUDE_NAMES,
                            "otel.exporter.cloud-logging.metrics.exclude.names", "metric1,metric2"),
                         of(ExtensionConfigurations.EXPORTER.CLOUD_LOGGING.METRICS.INCLUDE_NAMES,
                            "otel.exporter.cloud-logging.metrics.include.names", "metric1,metric2"),
                         of(ExtensionConfigurations.EXPORTER.DYNATRACE.METRICS.EXCLUDE_NAMES,
                            "otel.exporter.dynatrace.metrics.exclude.names", "metric1,metric2"),
                         of(ExtensionConfigurations.EXPORTER.DYNATRACE.METRICS.INCLUDE_NAMES,
                            "otel.exporter.dynatrace.metrics.include.names", "metric1,metric2"));
    }

    @ParameterizedTest
    @MethodSource("provideStringProperties")
    void retrievesStringValueCorrectly(ConfigProperty<String> property, String key, String expectedValue) {
        DefaultConfigProperties config = createFromMap(Collections.singletonMap(key, expectedValue));
        String actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @ParameterizedTest
    @MethodSource("provideStringProperties")
    void retrievesStringDefaultValueCorrectly(ConfigProperty<String> property, String key, String expectedValue) {
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        String actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(property.getDefaultValue());
    }

    @ParameterizedTest
    @MethodSource("provideBooleanProperties")
    void retrievesBooleanValueCorrectly(ConfigProperty<Boolean> property, String key, String value) {
        DefaultConfigProperties config = createFromMap(Collections.singletonMap(key, value));
        Boolean actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(Boolean.parseBoolean(value));
    }

    @ParameterizedTest
    @MethodSource("provideBooleanProperties")
    void retrievesBooleanDefaultValueCorrectly(ConfigProperty<Boolean> property, String key, String expectedValue) {
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        Boolean actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(property.getDefaultValue());
    }

    @ParameterizedTest
    @MethodSource("provideDurationProperties")
    void retrievesDurationValueCorrectrly(ConfigProperty<Duration> property, String key, String value) {
        DefaultConfigProperties config = createFromMap(Collections.singletonMap(key, value));
        Duration actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(Duration.ofMillis(Long.parseLong(value)));
    }

    @ParameterizedTest
    @MethodSource("provideDurationProperties")
    void retrievesDurationDefaultValueCorrectly(ConfigProperty<Duration> property, String key, String expectedValue) {
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        Duration actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(property.getDefaultValue());
    }

    @ParameterizedTest
    @MethodSource("provideListProperties")
    void retrievesListValueCorrectly(ConfigProperty<List<String>> property, String key, String value) {
        DefaultConfigProperties config = createFromMap(Collections.singletonMap(key, value));
        List<String> actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(Arrays.asList(value.split(",")));
    }

    @ParameterizedTest
    @MethodSource("provideListProperties")
    void retrievesListDefaultValueCorrectly(ConfigProperty<List<String>> property, String key, String expectedValue) {
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        List<String> actualValue = property.getValue(config);
        assertThat(actualValue).isEqualTo(property.getDefaultValue());
    }

    private DefaultConfigProperties createFromMap(Map<String, String> properties) {
        return DefaultConfigProperties.createFromMap(properties);
    }
}
