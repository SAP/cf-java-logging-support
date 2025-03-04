package com.sap.hcp.cf.log4j2.layout.suppliers;

import com.sap.hcp.cf.logging.common.Markers;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

public final class LogEventUtilities {

    private LogEventUtilities() {
    }

    public static String getFormattedMessage(LogEvent event) {
        Message message = event.getMessage();
        return message == null ? null : message.getFormattedMessage();
    }

    public static Object[] getParameterArray(LogEvent event) {
        Message message = event.getMessage();
        return message == null ? null : message.getParameters();
    }

    public static boolean isRequestLog(LogEvent event) {
        return event.getMarker() != null && Markers.REQUEST_MARKER.getName().equals(event.getMarker().getName());
    }
}
