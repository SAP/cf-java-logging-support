package com.sap.hcp.cf.log4j2.layout.supppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BaseFieldSupplier implements Log4jContextFieldSupplier {

    @Override
    public Map<String, Object> map(LogEvent event) {
        Map<String, Object> fields = new HashMap<>(6);
        fields.put(Fields.WRITTEN_AT, getIsoTs(event));
        fields.put(Fields.WRITTEN_TS, getNanoTs(event));
        fields.put(Fields.TYPE, LogEventUtilities.isRequestLog(event) ? Defaults.TYPE_REQUEST : Defaults.TYPE_LOG);
        fields.put(Fields.LEVEL, String.valueOf(event.getLevel()));
        fields.put(Fields.LOGGER, event.getLoggerName());
        fields.put(Fields.THREAD, event.getThreadName());
        if (!LogEventUtilities.isRequestLog(event) && event.getMessage() != null) {
            fields.put(Fields.MSG, LogEventUtilities.getFormattedMessage(event));
        }
        if (event.getThrown() != null) {
            Throwable throwable = event.getThrown();
            fields.put(Fields.EXCEPTION_TYPE, throwable.getClass().getName());
            if (StringUtils.isNotBlank(throwable.getMessage())) {
                fields.put(Fields.EXCEPTION_MESSAGE, throwable.getMessage());
            }
        }
        return fields;
    }

    private String getIsoTs(LogEvent event) {
        org.apache.logging.log4j.core.time.Instant instant = event.getInstant();
        return Instant.ofEpochSecond(instant.getEpochSecond(), instant.getNanoOfSecond()).toString();
    }

    private String getNanoTs(LogEvent event) {
        org.apache.logging.log4j.core.time.Instant instant = event.getInstant();
        return String.valueOf(instant.getEpochSecond() * 1_000_000_000L + instant.getNanoOfSecond());
    }
}
