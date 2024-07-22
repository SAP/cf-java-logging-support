package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.customfields.CustomField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventContextFieldSupplierTest {

    private final LogbackContextFieldSupplier fieldSupplier = new EventContextFieldSupplier();
    @Mock
    private ILoggingEvent event;

    @Test
    public void emptyMdcAndNoArguments() {
        when(event.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).isEmpty();
    }

    @Test
    public void mdcFields() throws Exception {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put("key", "value");
        mdc.put("this", "that");
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).containsEntry("key", "value");
        assertThat(fields).containsEntry("this", "that");
    }

    @Test
    public void customFields() throws Exception {
        Object[] arguments = new Object[] { //
                                            new Object(), //
                                            CustomField.customField("key", "value"), //
                                            CustomField.customField("number", 123.456d) };
        when(event.getArgumentArray()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).containsEntry("key", "value");
        assertThat(fields).containsEntry("number", 123.456d);
    }

    @Test
    public void customFieldOverwritesMdc() throws Exception {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put("key", "this");
        when(event.getMDCPropertyMap()).thenReturn(mdc);
        Object[] arguments = new Object[] { CustomField.customField("key", "that") };
        when(event.getArgumentArray()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);

        assertThat(fields).containsEntry("key", "that");
    }

}
