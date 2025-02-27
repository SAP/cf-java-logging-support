package com.sap.hcp.cf.log4j2.layout.suppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.request.RequestRecord;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RequestRecordFieldSupplierTest {

    private static final Marker MARKER = MarkerManager.getMarker(Markers.REQUEST_MARKER.getName());
    private final Log4jContextFieldSupplier fieldSupplier = new RequestRecordFieldSupplier();

    private static Builder requestLogEventBuilder() {
        return Log4jLogEvent.newBuilder().setMarker(MARKER);
    }

    @Test
    public void nullArgumentArray() {
        LogEvent event = requestLogEventBuilder().setMessage(new SimpleMessage()).build();
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).isEmpty();
    }

    @Test
    public void emptyArgumentArray() {
        MutableLogEvent event = new MutableLogEvent(new StringBuilder(), new Object[0]);
        event.setMarker(MARKER);
        Map<String, Object> fields = fieldSupplier.map(event.createMemento());
        assertThat(fields).isEmpty();
    }

    @Test
    public void requestRecordArgument() {
        RequestRecord requestRecord = requestRecord("test").build();
        Message message = new ParameterizedMessage("", requestRecord);
        LogEvent event = requestLogEventBuilder().setMessage(message).build();
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).containsEntry(Fields.LAYER, "test");
    }

    @Test
    public void requestRecordMessageText() {
        RequestRecord requestRecord = requestRecord("test").build();
        SimpleMessage message = new SimpleMessage(requestRecord.toString());
        LogEvent event = requestLogEventBuilder().setMessage(message).build();
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).containsEntry(Fields.LAYER, "test");
    }

}
