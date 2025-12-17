package com.sap.hcf.cf.logging.opentelemetry.agent.ext.config;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties.createFromMap;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigPropertyTest {

    @Test
    void returnsDefaultWhenConfigIsNull() {
        ConfigProperty<String> prop =
                ConfigProperty.stringValued("non.existent.key").withDefaultValue("defaultValue").build();
        String value = prop.getValue(null);
        assertThat(value).isEqualTo("defaultValue");
    }

    @Test
    void returnsNullWhenDefaultIsNull() {
        ConfigProperty<String> prop = ConfigProperty.stringValued("non.existent.key").build();
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        String value = prop.getValue(config);
        assertThat(value).isNull();
    }

    @Test
    void returnsDefaultStringWhenNotSet() {
        ConfigProperty<String> prop =
                ConfigProperty.stringValued("non.existent.key").withDefaultValue("defaultValue").build();
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        String value = prop.getValue(config);
        assertThat(value).isEqualTo("defaultValue");
    }

    @Test
    void returnsStringWhenSet() {
        ConfigProperty<String> prop =
                ConfigProperty.stringValued("existent.key").withDefaultValue("defaultValue").build();
        DefaultConfigProperties config = createFromMap(Collections.singletonMap("existent.key", "actualValue"));
        String value = prop.getValue(config);
        assertThat(value).isEqualTo("actualValue");
    }

    @Test
    void returnsFallbackStringWhenNotSet() {
        ConfigProperty<String> fallbackProp =
                ConfigProperty.stringValued("fallback.key").withDefaultValue("defaultValue").build();
        ConfigProperty<String> prop =
                ConfigProperty.stringValued("non.existent.key").withFallback(fallbackProp).build();
        DefaultConfigProperties config = createFromMap(Collections.singletonMap("fallback.key", "fallbackValue"));
        String value = prop.getValue(config);
        assertThat(value).isEqualTo("fallbackValue");
    }

    @Test
    void primaryKeyOverridesFallback() {
        ConfigProperty<String> fallbackProp =
                ConfigProperty.stringValued("fallback.key").withDefaultValue("defaultValue").build();
        ConfigProperty<String> prop = ConfigProperty.stringValued("primary.key").withFallback(fallbackProp).build();
        DefaultConfigProperties config = createFromMap(new java.util.HashMap<String, String>() {{
            put("primary.key", "primaryValue");
            put("fallback.key", "fallbackValue");
        }});
        String value = prop.getValue(config);
        assertThat(value).isEqualTo("primaryValue");
    }

    @Test
    void returnsDefaultBooleanWhenNotSet() {
        ConfigProperty<Boolean> prop = ConfigProperty.booleanValued("non.existent.key").withDefaultValue(true).build();
        DefaultConfigProperties config = createFromMap(Collections.emptyMap());
        Boolean value = prop.getValue(config);
        assertThat(value).isTrue();
    }

    @Test
    void returnsBooleanWhenSet() {
        ConfigProperty<Boolean> prop = ConfigProperty.booleanValued("existent.key").withDefaultValue(true).build();
        DefaultConfigProperties config = createFromMap(Collections.singletonMap("existent.key", "false"));
        Boolean value = prop.getValue(config);
        assertThat(value).isFalse();
    }

    @Test
    void returnsFallbackBooleanWhenNotSet() {
        ConfigProperty<Boolean> fallbackProp = ConfigProperty.booleanValued("fallback.key").build();
        ConfigProperty<Boolean> prop =
                ConfigProperty.booleanValued("non.existent.key").withFallback(fallbackProp).build();
        DefaultConfigProperties config = createFromMap(Collections.singletonMap("fallback.key", "true"));
        Boolean value = prop.getValue(config);
        assertThat(value).isTrue();
    }
}
