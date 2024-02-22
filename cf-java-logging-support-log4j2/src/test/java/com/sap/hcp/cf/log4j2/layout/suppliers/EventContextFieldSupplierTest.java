package com.sap.hcp.cf.log4j2.layout.suppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.EventContextFieldSupplier;
import com.sap.hcp.cf.logging.common.customfields.CustomField;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventContextFieldSupplierTest {

    private final Log4jContextFieldSupplier fieldSupplier = new EventContextFieldSupplier();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LogEvent event;

    @Test
    public void emptyMdcAndNoArguments() {
        when(event.getContextData().toMap()).thenReturn(Collections.emptyMap());
        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).isEmpty();
    }

    @Test
    public void mdcFields() throws Exception {
        when(event.getContextData().toMap()).thenReturn(Map.ofEntries(entry("key", "value"), entry("this", "that")));

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).containsEntry("key", "value").containsEntry("this", "that");
    }

    @Test
    public void customFields() throws Exception {
        Object[] arguments = new Object[] { //
                                            new Object(), //
                                            CustomField.customField("key", "value"), //
                                            CustomField.customField("this", 123.456d) };
        when(event.getMessage().getParameters()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).containsEntry("key", "value").containsEntry("this", 123.456d);
    }

    @Test
    public void customFieldOverwritesMdc() throws Exception {
        when(event.getContextData().toMap()).thenReturn(Map.ofEntries(entry("key", "this")));
        Object[] arguments = new Object[] { CustomField.customField("key", "that") };
        when(event.getMessage().getParameters()).thenReturn(arguments);

        Map<String, Object> fields = fieldSupplier.map(event);
        assertThat(fields).containsEntry("key", "that");
    }

}
