package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

import java.util.Collection;

class NoopLogRecordExporter implements LogRecordExporter {
    private static final LogRecordExporter INSTANCE = new NoopLogRecordExporter();

    NoopLogRecordExporter() {
    }

    static LogRecordExporter getInstance() {
        return INSTANCE;
    }

    public CompletableResultCode export(Collection<LogRecordData> logs) {
        return CompletableResultCode.ofSuccess();
    }

    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
