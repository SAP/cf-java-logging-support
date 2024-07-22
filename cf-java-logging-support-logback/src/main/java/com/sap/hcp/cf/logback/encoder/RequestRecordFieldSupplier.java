package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.AbstractRequestRecordFieldSupplier;

public class RequestRecordFieldSupplier extends AbstractRequestRecordFieldSupplier<ILoggingEvent>
        implements LogbackContextFieldSupplier {

    @Override
    protected boolean isRequestLog(ILoggingEvent event) {
        return ILoggingEventUtilities.isRequestLog(event);
    }

    @Override
    protected String getFormattedMessage(ILoggingEvent event) {
        return event.getFormattedMessage();
    }

    @Override
    protected Object[] getParameterArray(ILoggingEvent event) {
        return event.getArgumentArray();
    }

}
