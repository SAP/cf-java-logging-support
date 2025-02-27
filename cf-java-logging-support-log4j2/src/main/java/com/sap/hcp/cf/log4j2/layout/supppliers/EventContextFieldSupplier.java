package com.sap.hcp.cf.log4j2.layout.supppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.AbstractContextFieldSupplier;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Map;

public class EventContextFieldSupplier extends AbstractContextFieldSupplier<LogEvent>
        implements Log4jContextFieldSupplier {

    @Override
    public int order() {
        return Log4jContextFieldSupplier.CONTEXT_FIELDS;
    }

    @Override
    protected Map<String, String> getContextMap(LogEvent event) {
        return event.getContextData().toMap();
    }

    @Override
    protected Object[] getParameterArray(LogEvent event) {
        return LogEventUtilities.getParameterArray(event);
    }

}
