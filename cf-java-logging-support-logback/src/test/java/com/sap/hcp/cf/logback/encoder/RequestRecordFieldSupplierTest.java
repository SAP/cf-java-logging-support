package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Markers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestRecordFieldSupplierTest {

    private final LogbackContextFieldSupplier fieldSupplier = new RequestRecordFieldSupplier();
    @Mock
    private ILoggingEvent event;

    @Test
    public void nullArgumentArray() {
        when(event.getMarker()).thenReturn(Markers.REQUEST_MARKER);
        when(event.getFormattedMessage()).thenReturn("");

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).isEmpty();
    }

    @Test
    public void emptyArgumentArray() {
        when(event.getMarker()).thenReturn(Markers.REQUEST_MARKER);
        when(event.getFormattedMessage()).thenReturn("");
        when(event.getArgumentArray()).thenReturn(new Object[0]);

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).isEmpty();
    }

    @Test
    public void requestRecordArgument() {
        when(event.getMarker()).thenReturn(Markers.REQUEST_MARKER);
        when(event.getArgumentArray()).thenReturn(new Object[] { requestRecord("test").build() });

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).containsEntry("layer", "test");
    }

}
