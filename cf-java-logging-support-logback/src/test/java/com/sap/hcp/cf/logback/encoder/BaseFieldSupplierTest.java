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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseFieldSupplierTest {

    private final LogbackContextFieldSupplier baseFieldSupplier = new BaseFieldSupplier();
    @Mock
    private ILoggingEvent event;

    @Test
    public void addsNoExceptionFieldsWithoutException() {
        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields).doesNotContainKey(Fields.EXCEPTION_TYPE);
        assertThat(fields).doesNotContainKey(Fields.EXCEPTION_MESSAGE);
    }

    @Test
    public void mapsException() {
        Exception exception = new RuntimeException("exception message");
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(exception));

        Map<String, Object> fields = baseFieldSupplier.map(event);

        assertThat(fields).containsEntry(Fields.EXCEPTION_TYPE, RuntimeException.class.getName());
        assertThat(fields).containsEntry(Fields.EXCEPTION_MESSAGE, "exception message");
    }
}
