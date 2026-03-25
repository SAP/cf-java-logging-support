package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logging.common.Markers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ILoggingEventUtilitiesTest {

    @Mock
    private ILoggingEvent event;

    @Test
    void noMarkersIsNotRequqstLog() {
        assertFalse(ILoggingEventUtilities.isRequestLog(event));
    }

    @Test
    void emptyMarkerListIsNotRequestLog() {
        when(event.getMarkerList()).thenReturn(Collections.emptyList());
        assertFalse(ILoggingEventUtilities.isRequestLog(event));
    }

    @Test
    void withRequestMarkerIsNotRequestLog() {
        when(event.getMarkerList()).thenReturn(Arrays.asList(mock(Marker.class), Markers.REQUEST_MARKER));
        assertTrue(ILoggingEventUtilities.isRequestLog(event));
    }

    @Test
    void withOtherMarkerIsNotRequestLog() {
        when(event.getMarkerList()).thenReturn(Arrays.asList(mock(Marker.class), mock(Marker.class)));
        assertFalse(ILoggingEventUtilities.isRequestLog(event));
    }
}
