package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * A customizer that redacts the text of database connect statements in span attributes. This is to avoid leaking
 * sensitive information in traces. The customizer checks for the presence of the "db.query.text" and "db.statement"
 * attributes, and if they start with "connect", it replaces the rest of the string with "[REDACTED]". The customizer
 * can be enabled or disabled via the configuration property "sap.cf.integration.otel.extension.sanitizer.enabled". By
 * default, it is enabled.
 */
public class DbConnectStatementCustomizer implements SpanAttributeCustomizer {

    private static final AttributeKey<String> DB_QUERY_TEXT = stringKey("db.query.text");
    //@Deprecated
    private static final AttributeKey<String> DB_STATEMENT = stringKey("db.statement");
    private static final String REDACTED = " [REDACTED]";

    @Override
    public boolean isEnabled(ConfigProperties config) {
        return ExtensionConfigurations.EXTENSION.SANITIZER.ENABLED.getValue(config);
    }

    @Override
    public boolean isApplicable(Attributes original) {
        String dbQueryText = original.get(DB_QUERY_TEXT);
        String dbStatement = original.get(DB_STATEMENT);
        return isCritical(dbQueryText) || isCritical(dbStatement);
    }

    private boolean isCritical(String query) {
        return query != null && query.toLowerCase().startsWith("connect");
    }

    @Override
    public void customize(AttributesBuilder builder, Attributes original) {
        String dbQueryText = original.get(DB_QUERY_TEXT);
        String dbStatement = original.get(DB_STATEMENT);
        if (isCritical(dbQueryText)) {
            builder.put(DB_QUERY_TEXT, dbQueryText.substring(0, 7) + REDACTED);
        }
        if (isCritical(dbStatement)) {
            builder.put(DB_STATEMENT, dbStatement.substring(0, 7) + REDACTED);
        }
    }
}
