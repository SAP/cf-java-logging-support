package com.sap.hcp.cf.logback.converter.api;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logging.common.serialization.EventContextFieldSupplier;

@FunctionalInterface
public interface LogbackContextFieldSupplier extends EventContextFieldSupplier<ILoggingEvent> {

    int BASE_FIELDS = -100;
    int CONTEXT_FIELDS = -90;
    int REQUEST_FIELDS = -80;
}
