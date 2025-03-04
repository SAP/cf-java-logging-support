package com.sap.hcp.cf.log4j2.layout.suppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Fields;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseFieldSupplierTest {

    private final Log4jContextFieldSupplier baseFieldSupplier = new BaseFieldSupplier();
    @Mock
    private LogEvent event;

    @BeforeEach
    public void initializeEvent() {
        when(event.getInstant()).thenReturn(mock(Instant.class));
    }

    @Test
    public void addsNoExceptionFieldsWithoutException() {
        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields).doesNotContainKey(Fields.EXCEPTION_TYPE);
        assertThat(fields).doesNotContainKey(Fields.EXCEPTION_MESSAGE);
    }

    @Test
    public void mapsException() {
        Exception exception = new RuntimeException("exception message");
        when(event.getThrown()).thenReturn(exception);

        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields).containsEntry(Fields.EXCEPTION_TYPE, RuntimeException.class.getName());
        assertThat(fields).containsEntry(Fields.EXCEPTION_MESSAGE, "exception message");
    }
}
