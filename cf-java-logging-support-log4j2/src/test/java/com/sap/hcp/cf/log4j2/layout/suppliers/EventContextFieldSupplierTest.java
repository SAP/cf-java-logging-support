package com.sap.hcp.cf.log4j2.layout.suppliers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.EventContextFieldSupplier;
import com.sap.hcp.cf.logging.common.customfields.CustomField;

@ExtendWith(MockitoExtension.class)
public class EventContextFieldSupplierTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LogEvent event;

    private Log4jContextFieldSupplier fieldSupplier = new EventContextFieldSupplier();

    @Test
    public void emptyMdcAndNoArguments() {
        when(event.getContextData().toMap()).thenReturn(Collections.emptyMap());
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields.entrySet(), is(empty()));
    }

    @Test
    public void mdcFields() throws Exception {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put("key", "value");
        mdc.put("this", "that");
        when(event.getContextData().toMap()).thenReturn(mdc);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("key", "value"));
        assertThat(fields, hasEntry("this", "that"));
    }

    @Test
    public void customFields() throws Exception {
        Object[] arguments = new Object[] { //
                                            new Object(), //
                                            CustomField.customField("key", "value"), //
                                            CustomField.customField("this", Double.valueOf(123.456d)) };
        when(event.getMessage().getParameters()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("key", "value"));
        assertThat(fields, hasEntry("this", Double.valueOf(123.456d)));
    }

    @Test
    public void customFieldOverwritesMdc() throws Exception {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put("key", "this");
        when(event.getContextData().toMap()).thenReturn(mdc);
        Object[] arguments = new Object[] { CustomField.customField("key", "that") };
        when(event.getMessage().getParameters()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields, hasEntry("key", "that"));
    }

}
