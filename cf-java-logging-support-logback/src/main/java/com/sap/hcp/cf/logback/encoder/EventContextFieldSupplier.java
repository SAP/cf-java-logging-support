package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.AbstractContextFieldSupplier;

import java.util.Map;

public class EventContextFieldSupplier extends AbstractContextFieldSupplier<ILoggingEvent>
        implements LogbackContextFieldSupplier {

    @Override
    protected Object[] getParameterArray(ILoggingEvent event) {
        return event.getArgumentArray();
    }

    @Override
    protected Map<? extends String, ? extends Object> getContextMap(ILoggingEvent event) {
        return event.getMDCPropertyMap();
    }

}
