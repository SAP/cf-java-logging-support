package com.sap.hcp.cf.log4j2.converter.api;

import com.sap.hcp.cf.logging.common.serialization.EventContextFieldSupplier;
import org.apache.logging.log4j.core.LogEvent;

@FunctionalInterface
public interface Log4jContextFieldSupplier extends EventContextFieldSupplier<LogEvent> {

    int BASE_FIELDS = -100;
    int CONTEXT_FIELDS = -90;
    int REQUEST_FIELDS = -80;

}
