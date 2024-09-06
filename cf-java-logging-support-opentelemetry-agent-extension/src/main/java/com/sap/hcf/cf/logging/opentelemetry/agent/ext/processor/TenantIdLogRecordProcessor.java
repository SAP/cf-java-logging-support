package com.sap.hcf.cf.logging.opentelemetry.agent.ext.processor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;

import java.util.logging.Logger;

public class TenantIdLogRecordProcessor implements LogRecordProcessor {

    private static final Logger LOG = Logger.getLogger(TenantIdLogRecordProcessor.class.getName());

    private static final AttributeKey<String> TENANTID_ATTR = AttributeKey.stringKey("tenantid");

    @Override
    public void onEmit(Context context, ReadWriteLogRecord logRecord) {
        String rootTenantid = getTenantIdFromLocalRootSpan(context);
        if (rootTenantid != null) {
            logRecord.setAttribute(TENANTID_ATTR, rootTenantid);
            return;
        }
        String currentTenantid = getTenantIdFromCurrentSpan(context);
        if (currentTenantid != null) {
            logRecord.setAttribute(TENANTID_ATTR, currentTenantid);
            return;
        }
        LOG.fine("Cannot determine tenant id for log record.");
    }

    private String getTenantIdFromCurrentSpan(Context context) {
        Span current = Span.fromContextOrNull(context);
        return current == null ? null : getTenantId(current);
    }

    private String getTenantIdFromLocalRootSpan(Context context) {
        Span root = LocalRootSpan.fromContextOrNull(context);
        return root == null ? null : getTenantId(root);
    }

    private final String getTenantId(Span span) {
        if (span instanceof ReadableSpan) {
            ReadableSpan readSpan = (ReadableSpan) span;
            String tenantId = readSpan.getAttribute(TENANTID_ATTR);
            if (tenantId != null && !tenantId.isEmpty()) {
                return tenantId;
            }
            LOG.finer(() -> "No tenant id found in span " + readSpan.getSpanContext().getSpanId() + ".");
        } else {
            LOG.finer(() -> "Span " + span.getSpanContext().getSpanId() + " is not readable.");
        }
        return null;
    }
}
