package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class SanitizeSpanExporterCustomizer implements BiFunction<SpanExporter, ConfigProperties, SpanExporter> {

    private static final String PROPERTY_ENABLED_KEY = "sap.cf.integration.otel.extension.sanitizer.enabled";
    private static final AttributeKey<String> DB_QUERY_TEXT = stringKey("db.query.text");
    //@Deprecated
    private static final AttributeKey<String> DB_STATEMENT = stringKey("db.statement");

    @Override
    public SpanExporter apply(SpanExporter delegate, ConfigProperties config) {
        if (config != null && !config.getBoolean(PROPERTY_ENABLED_KEY, true)) {
            return delegate;
        }
        return new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                return delegate.export(spans.stream().map(this::sanitizeSpanData).collect(Collectors.toList()));
            }

            private SpanData sanitizeSpanData(SpanData spanData) {
                Attributes attributes = spanData.getAttributes();
                if (attributes == null) {
                    return spanData;
                }
                String dbQueryText = attributes.get(DB_QUERY_TEXT);
                String dbStatement = attributes.get(DB_STATEMENT);
                if (isClean(dbQueryText) && isClean(dbStatement)) {
                    return spanData;
                }
                AttributesBuilder sanitized = attributes.toBuilder();
                if (!isClean(dbQueryText)) {
                    sanitized.put(DB_QUERY_TEXT, dbQueryText.substring(0, 7) + " [REDACTED]");
                }
                if (!isClean(dbStatement)) {
                    sanitized.put(DB_STATEMENT, dbStatement.substring(0, 7) + " [REDACTED]");
                }
                return new SanitizedSpanData(spanData, sanitized.build());
            }

            private boolean isClean(String query) {
                return query == null || !query.toLowerCase().startsWith("connect");
            }

            @Override
            public CompletableResultCode flush() {
                return delegate.flush();
            }

            @Override
            public CompletableResultCode shutdown() {
                return delegate.shutdown();
            }
        };
    }

    private static class SanitizedSpanData extends DelegatingSpanData {

        private final Attributes filteredAttributes;

        protected SanitizedSpanData(SpanData delegate, Attributes filteredAttrinutes) {
            super(delegate);
            this.filteredAttributes = filteredAttrinutes;
        }

        @Override
        public Attributes getAttributes() {
            return filteredAttributes;
        }
    }
}
