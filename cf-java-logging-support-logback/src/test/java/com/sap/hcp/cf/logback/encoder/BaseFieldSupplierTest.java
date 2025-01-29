package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Fields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseFieldSupplierTest {

    @Mock
    private ILoggingEvent event;

    private LogbackContextFieldSupplier baseFieldSupplier = new BaseFieldSupplier();

    @Test
    public void addsNoExceptionFieldsWithoutException() {
        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields, not(hasKey(Fields.EXCEPTION_TYPE)));
        assertThat(fields, not(hasKey(Fields.EXCEPTION_MESSAGE)));
    }

    @Test
    public void mapsException() {
        Exception exception = new RuntimeException("exception message");
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(exception));

        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields, hasEntry(Fields.EXCEPTION_TYPE, RuntimeException.class.getName()));
        assertThat(fields, hasEntry(Fields.EXCEPTION_MESSAGE, "exception message"));
    }
}