package com.sap.hcp.cf.log4j2.layout.supppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.AbstractRequestRecordFieldSupplier;
import org.apache.logging.log4j.core.LogEvent;

public class RequestRecordFieldSupplier extends AbstractRequestRecordFieldSupplier<LogEvent>
        implements Log4jContextFieldSupplier {

    @Override
    protected boolean isRequestLog(LogEvent event) {
        return LogEventUtilities.isRequestLog(event);
    }

    @Override
    protected String getFormattedMessage(LogEvent event) {
        return LogEventUtilities.getFormattedMessage(event);
    }

    @Override
    protected Object[] getParameterArray(LogEvent event) {
        return LogEventUtilities.getParameterArray(event);
    }

}
