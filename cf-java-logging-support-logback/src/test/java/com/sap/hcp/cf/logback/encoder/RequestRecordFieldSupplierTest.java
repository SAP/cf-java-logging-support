package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Markers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestRecordFieldSupplierTest {

    private static final List<Marker> REQUEST_MARKER = Collections.singletonList(Markers.REQUEST_MARKER);

    private final LogbackContextFieldSupplier fieldSupplier = new RequestRecordFieldSupplier();
    @Mock
    private ILoggingEvent event;

    @Test
    public void nullArgumentArray() {
        when(event.getMarkerList()).thenReturn(REQUEST_MARKER);
        when(event.getFormattedMessage()).thenReturn("");

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).isEmpty();
    }

    @Test
    public void emptyArgumentArray() {
        when(event.getMarkerList()).thenReturn(REQUEST_MARKER);
        when(event.getFormattedMessage()).thenReturn("");
        when(event.getArgumentArray()).thenReturn(new Object[0]);

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).isEmpty();
    }

    @Test
    public void requestRecordArgument() {
        when(event.getMarkerList()).thenReturn(REQUEST_MARKER);
        when(event.getArgumentArray()).thenReturn(new Object[] { requestRecord("test").build() });

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).containsEntry("layer", "test");
    }

}
