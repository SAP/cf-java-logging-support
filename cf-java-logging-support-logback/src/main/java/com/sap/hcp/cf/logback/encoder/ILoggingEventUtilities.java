package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logging.common.Markers;

import java.util.Map;

public final class ILoggingEventUtilities {

    private ILoggingEventUtilities() {
    }

    public static boolean isRequestLog(ILoggingEvent event) {
        return Markers.REQUEST_MARKER.equals(event.getMarker());
    }

    public static Map<?, ?> getMap(ILoggingEvent event) {
        return event.getMDCPropertyMap();
    }

}
