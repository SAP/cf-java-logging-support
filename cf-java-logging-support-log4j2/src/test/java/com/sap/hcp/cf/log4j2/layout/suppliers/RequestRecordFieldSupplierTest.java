package com.sap.hcp.cf.log4j2.layout.suppliers;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.Map;

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

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.RequestRecordFieldSupplier;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.request.RequestRecord;

@ExtendWith(MockitoExtension.class)
public class RequestRecordFieldSupplierTest {

    private static final Marker MARKER = MarkerManager.getMarker(Markers.REQUEST_MARKER.getName());
    private Log4jContextFieldSupplier fieldSupplier = new RequestRecordFieldSupplier();

    @Test
    public void nullArgumentArray() {
        LogEvent event = requestLogEventBuilder().setMessage(new SimpleMessage()).build();
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields.entrySet(), is(empty()));
    }

    private static Builder requestLogEventBuilder() {
        return Log4jLogEvent.newBuilder().setMarker(MARKER);
    }

    @Test
    public void emptyArgumentArray() {
        MutableLogEvent event = new MutableLogEvent(new StringBuilder(""), new Object[0]);
        event.setMarker(MARKER);
        Map<String, Object> fields = fieldSupplier.map(event.createMemento());
        assertThat(fields.entrySet(), is(empty()));
    }

    @Test
    public void requestRecordArgument() {
        RequestRecord requestRecord = requestRecord("test").build();
        Message message = new ParameterizedMessage("", requestRecord);
        LogEvent event = requestLogEventBuilder().setMessage(message).build();
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("layer", "test"));
    }


    @Test
    public void requestRecordMessageText() {
        RequestRecord requestRecord = requestRecord("test").build();
        SimpleMessage message = new SimpleMessage(requestRecord.toString());
        LogEvent event = requestLogEventBuilder().setMessage(message).build();
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("layer", "test"));
    }

}
