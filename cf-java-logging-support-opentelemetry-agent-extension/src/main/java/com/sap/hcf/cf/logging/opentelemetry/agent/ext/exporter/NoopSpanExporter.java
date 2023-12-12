package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;

class NoopSpanExporter implements SpanExporter {
    private static final SpanExporter INSTANCE = new NoopSpanExporter();

    NoopSpanExporter() {
    }

    static SpanExporter getInstance() {
        return INSTANCE;
    }

    public CompletableResultCode export(Collection<SpanData> logs) {
        return CompletableResultCode.ofSuccess();
    }

    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
