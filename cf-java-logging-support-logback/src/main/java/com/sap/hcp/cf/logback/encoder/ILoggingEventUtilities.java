package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logging.common.Markers;
import org.slf4j.Marker;

import java.util.List;
import java.util.Map;

public final class ILoggingEventUtilities {

    private ILoggingEventUtilities() {
    }

    public static boolean isRequestLog(ILoggingEvent event) {
        List<Marker> markerList = event.getMarkerList();
        if (markerList == null || markerList.isEmpty()) {
            return false;
        }
        return markerList.stream().anyMatch(Markers.REQUEST_MARKER::equals);
    }

    public static Map<?, ?> getMap(ILoggingEvent event) {
        return event.getMDCPropertyMap();
    }

}
