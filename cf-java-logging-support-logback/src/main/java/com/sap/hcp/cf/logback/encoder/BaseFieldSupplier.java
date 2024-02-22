package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Markers;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BaseFieldSupplier implements LogbackContextFieldSupplier {

    @Override
    public Map<String, Object> map(ILoggingEvent event) {
        Map<String, Object> fields = new HashMap<>(6);
        fields.put(Fields.WRITTEN_AT, Instant.ofEpochMilli(event.getTimeStamp()).toString());
        fields.put(Fields.WRITTEN_TS, now());
        fields.put(Fields.TYPE, isRequestLog(event) ? Defaults.TYPE_REQUEST : Defaults.TYPE_LOG);
        fields.put(Fields.LEVEL, String.valueOf(event.getLevel()));
        fields.put(Fields.LOGGER, event.getLoggerName());
        fields.put(Fields.THREAD, event.getThreadName());
        if (!isRequestLog(event)) {
            fields.put(Fields.MSG, event.getFormattedMessage());
        }
        if (event.getThrowableProxy() != null && event.getThrowableProxy() instanceof ThrowableProxy) {
            Throwable throwable = ((ThrowableProxy) event.getThrowableProxy()).getThrowable();
            fields.put(Fields.EXCEPTION_TYPE, throwable.getClass().getName());
            if (StringUtils.isNotBlank(throwable.getMessage())) {
                fields.put(Fields.EXCEPTION_MESSAGE, throwable.getMessage());
            }
        }
        return fields;
    }

    private String now() {
        Instant now = Instant.now();
        long timestamp = now.getEpochSecond() * 1_000_000_000L + now.getNano();
        return String.valueOf(timestamp);
    }

    private boolean isRequestLog(ILoggingEvent event) {
        return Markers.REQUEST_MARKER.equals(event.getMarker());
    }

}
