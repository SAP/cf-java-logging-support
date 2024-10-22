package com.sap.hcp.cf.logback.encoder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.customfields.CustomField;

import ch.qos.logback.classic.spi.ILoggingEvent;

@RunWith(MockitoJUnitRunner.class)
public class EventContextFieldSupplierTest {

    @Mock
    private ILoggingEvent event;

    private LogbackContextFieldSupplier fieldSupplier = new EventContextFieldSupplier();

    @Test
    public void emptyMdcAndNoArguments() {
        when(event.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields.entrySet(), is(empty()));
    }

    @Test
    public void mdcFields() throws Exception {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put("key", "value");
        mdc.put("this", "that");
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("key", "value"));
        assertThat(fields, hasEntry("this", "that"));
    }

    @Test
    public void customFields() throws Exception {
        Object[] arguments = new Object[] { //
                                            new Object(), //
                                            CustomField.customField("key", "value"), //
                                            CustomField.customField("number", Double.valueOf(123.456d)) };
        when(event.getArgumentArray()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("key", "value"));
        assertThat(fields, hasEntry("number", Double.valueOf(123.456d)));
    }

    @Test
    public void customFieldOverwritesMdc() throws Exception {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put("key", "this");
        when(event.getMDCPropertyMap()).thenReturn(mdc);
        Object[] arguments = new Object[] { CustomField.customField("key", "that") };
        when(event.getArgumentArray()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("key", "that"));
    }

}
