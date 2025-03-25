package com.sap.hcp.cf.logback.encoder;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Markers;

import ch.qos.logback.classic.spi.ILoggingEvent;

@ExtendWith(MockitoExtension.class)
public class RequestRecordFieldSupplierTest {

    @Mock
    private ILoggingEvent event;

    private LogbackContextFieldSupplier fieldSupplier = new RequestRecordFieldSupplier();

    @Test
    public void nullArgumentArray() {
        when(event.getMarker()).thenReturn(Markers.REQUEST_MARKER);
        when(event.getFormattedMessage()).thenReturn("");
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields.entrySet(), is(empty()));
    }

    @Test
    public void emptyArgumentArray() {
        when(event.getMarker()).thenReturn(Markers.REQUEST_MARKER);
        when(event.getFormattedMessage()).thenReturn("");
        when(event.getArgumentArray()).thenReturn(new Object[0]);
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields.entrySet(), is(empty()));
    }

    @Test
    public void requestRecordArgument() {
        when(event.getMarker()).thenReturn(Markers.REQUEST_MARKER);
        when(event.getArgumentArray()).thenReturn(new Object[] { requestRecord("test").build() });
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("layer", "test"));
    }

}
