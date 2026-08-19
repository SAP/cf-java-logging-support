package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbConnectStatementCustomizerTest {

    private static final DbConnectStatementCustomizer CUSTOMIZER = new DbConnectStatementCustomizer();

    @Test
    void canBeDisabled() {
        Map<String, String> configEntries = new HashMap<>();
        configEntries.put("sap.cf.integration.otel.extension.sanitizer.enabled", "false");
        DefaultConfigProperties configProperties = DefaultConfigProperties.createFromMap(configEntries);
        assertFalse(CUSTOMIZER.isEnabled(configProperties));
        assertTrue(CUSTOMIZER.isEnabled(DefaultConfigProperties.createFromMap(new HashMap<>())));
    }

    @Test
    void isApplicableOnConnectStatements() {
        assertTrue(CUSTOMIZER.isApplicable(Attributes.builder().put("db.query.text", "connect to database").build()));
        assertTrue(CUSTOMIZER.isApplicable(Attributes.builder().put("db.statement", "connect to database").build()));
    }

    @Test
    void notApplicableOnNonConnectDbStatements() {
        assertFalse(CUSTOMIZER.isApplicable(Attributes.builder().put("db.query.text", "select * from table").build()));
        assertFalse(CUSTOMIZER.isApplicable(
                Attributes.builder().put("db.statement", "insert into table values (1)").build()));
    }

    @Test
    void notApplicableOnNonDbStatements() {
        assertFalse(CUSTOMIZER.isApplicable(Attributes.builder().put("http.method", "GET").build()));
        assertFalse(CUSTOMIZER.isApplicable(Attributes.builder().put("key", "connect to somewhere").build()));
    }

    @Test
    void redactsConnectStatements() {
        Attributes original = Attributes.builder().put("db.query.text", "connect to database")
                                        .put("db.statement", "Connect to database").build();
        AttributesBuilder sanitized = original.toBuilder();
        CUSTOMIZER.customize(sanitized, original);
        assertThat(sanitized.build()).extracting(a -> a.get(AttributeKey.stringKey("db.query.text")))
                                     .isEqualTo("connect [REDACTED]");
        assertThat(sanitized.build()).extracting(a -> a.get(AttributeKey.stringKey("db.statement")))
                                     .isEqualTo("Connect [REDACTED]");
    }

    @Test
    void keepsNonConnectStatements() {
        Attributes original = Attributes.builder().put("db.query.text", "insert to database")
                                        .put("db.statement", "INSERT to database").build();
        AttributesBuilder sanitized = original.toBuilder();
        CUSTOMIZER.customize(sanitized, original);
        assertThat(sanitized.build()).extracting(a -> a.get(AttributeKey.stringKey("db.query.text")))
                                     .isEqualTo("insert to database");
        assertThat(sanitized.build()).extracting(a -> a.get(AttributeKey.stringKey("db.statement")))
                                     .isEqualTo("INSERT to database");

    }
}
