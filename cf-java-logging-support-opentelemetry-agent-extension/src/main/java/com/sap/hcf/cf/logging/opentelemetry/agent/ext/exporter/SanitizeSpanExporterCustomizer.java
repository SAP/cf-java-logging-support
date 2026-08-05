package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer.DbConnectStatementCustomizer;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.customizer.SpanAttributeCustomizer;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

public class SanitizeSpanExporterCustomizer implements BiFunction<SpanExporter, ConfigProperties, SpanExporter> {

    private final List<SpanAttributeCustomizer> customizers;

    public SanitizeSpanExporterCustomizer() {
        this(List.of(new DbConnectStatementCustomizer()));
    }

    SanitizeSpanExporterCustomizer(List<SpanAttributeCustomizer> customizers) {
        this.customizers = customizers;
    }

    @Override
    public SpanExporter apply(SpanExporter delegate, ConfigProperties config) {
        // Keep delegate exporter unwrapped if no customizers are provided.
        if (customizers == null || customizers.isEmpty()) {
            return delegate;
        }
        // Keep delegate exporter unwrapped if no customizers are enabled.
        final List<SpanAttributeCustomizer> enabledCustomizers =
                customizers.stream().filter(c -> c.isEnabled(config)).collect(toList());
        if (enabledCustomizers.isEmpty()) {
            return delegate;
        }
        return new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                return delegate.export(spans.stream().map(this::sanitizeSpanData).collect(toList()));
            }

            private SpanData sanitizeSpanData(SpanData spanData) {
                Attributes attributes = spanData.getAttributes();
                if (attributes == null) {
                    return spanData;
                }
                // Only create a new AttributesBuilder if at least one customizer is applicable to the attributes.
                AttributesBuilder sanitized = null;
                for (SpanAttributeCustomizer customizer: enabledCustomizers) {
                    if (customizer.isApplicable(attributes)) {
                        if (sanitized == null) {
                            sanitized = attributes.toBuilder();
                        }
                        customizer.customize(sanitized, attributes);
                    }
                }
                if (sanitized == null) {
                    return spanData;
                }
                return new SanitizedSpanData(spanData, sanitized.build());
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

        protected SanitizedSpanData(SpanData delegate, Attributes filteredAttributes) {
            super(delegate);
            this.filteredAttributes = filteredAttributes;
        }

        @Override
        public Attributes getAttributes() {
            return filteredAttributes;
        }
    }
}
