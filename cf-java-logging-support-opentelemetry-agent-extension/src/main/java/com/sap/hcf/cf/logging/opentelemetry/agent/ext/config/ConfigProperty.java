package com.sap.hcf.cf.logging.opentelemetry.agent.ext.config;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;

public class ConfigProperty<T> {

    private static final Logger LOG = Logger.getLogger(ConfigProperty.class.getName());

    private final String key;
    private final ConfigProperty<T> fallback;
    private final T defaultValue;
    private final BiFunction<ConfigProperties, String, T> extractor;
    private final boolean deprecated;

    private ConfigProperty(Builder<T> builder) {
        this.key = builder.key;
        this.fallback = builder.fallback;
        this.defaultValue = builder.defaultValue;
        this.extractor = builder.extractor;
        this.deprecated = builder.deprecated;
    }

    public T getValue(ConfigProperties config) {
        if (config == null) {
            LOG.warning(
                    "No configuration provided, using default value \"" + defaultValue + "\" for key: \"" + key + "\"");
            return defaultValue;
        }
        T directValue = extractor.apply(config, key);
        if (directValue != null) {
            return directValue;
        }
        if (fallback == null) {
            return defaultValue;
        }
        T fallbackValue = fallback.getValue(config);
        if (fallbackValue != null && !fallbackValue.equals(defaultValue)) {
            if (fallback.deprecated && LOG.isLoggable(Level.WARNING)) {
                LOG.warning(
                        "Using deprecated configuration key \"" + fallback.key + "\". Please migrate to key: \"" + key + "\"");
            }
            return fallbackValue;
        }
        return defaultValue;
    }

    public String getKey() {
        return key;
    }

    T getDefaultValue() {
        return defaultValue;
    }

    static Builder<String> stringValued(String key) {
        return new Builder<>(ConfigProperties::getString).withKey(key);
    }

    static Builder<Boolean> booleanValued(String key) {
        return new Builder<>(ConfigProperties::getBoolean).withKey(key);
    }

    static Builder<Duration> durationValued(String key) {
        return new Builder<>(ConfigProperties::getDuration).withKey(key);
    }

    static Builder<List<String>> listValued(String key) {
        return new Builder<>(ConfigProperties::getList).withKey(key).withDefaultValue(emptyList());
    }

    static class Builder<T> {
        private final BiFunction<ConfigProperties, String, T> extractor;
        private String key;
        private T defaultValue;
        private ConfigProperty<T> fallback;
        private boolean deprecated;

        Builder(BiFunction<ConfigProperties, String, T> extractor) {
            this.extractor = extractor;
        }

        private Builder<T> withKey(String key) {
            this.key = key;
            return this;
        }

        Builder<T> withDefaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        Builder<T> withFallback(ConfigProperty<T> fallback) {
            this.fallback = fallback;
            return this;
        }

        Builder<T> setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        ConfigProperty<T> build() {
            return new ConfigProperty<>(this);
        }
    }
}
