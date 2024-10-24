package com.sap.hcp.cf.log4j2.layout.suppliers;

import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.supppliers.BaseFieldSupplier;
import com.sap.hcp.cf.logging.common.Fields;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseFieldSupplierTest {

    @Mock
    private LogEvent event;

    private Log4jContextFieldSupplier baseFieldSupplier = new BaseFieldSupplier();

    @BeforeEach
    public void initializeEvent() {
        when(event.getInstant()).thenReturn(mock(Instant.class));
    }

    @Test
    public void addsNoExceptionFieldsWithoutException() {
        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields, not(hasKey(Fields.EXCEPTION_TYPE)));
        assertThat(fields, not(hasKey(Fields.EXCEPTION_MESSAGE)));
    }

    @Test
    public void mapsException() {
        Exception exception = new RuntimeException("exception message");
        when(event.getThrown()).thenReturn(exception);

        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields, hasEntry(Fields.EXCEPTION_TYPE, RuntimeException.class.getName()));
        assertThat(fields, hasEntry(Fields.EXCEPTION_MESSAGE, "exception message"));
    }
}