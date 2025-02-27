package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.KeyValuePair;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyValuePairsFieldSupplierTest {

    private final KeyValuePairsFieldSupplier supplier = new KeyValuePairsFieldSupplier();

    @Mock
    private ILoggingEvent event;

    @Test
    void nullEvent() {
        assertThat(supplier.map(null)).isEmpty();
    }

    @Test
    void nullKeyValuePairs() {
        when(event.getKeyValuePairs()).thenReturn(null);
        assertThat(supplier.map(event)).isEmpty();
    }

    @Test
    void emptyKeyValuePairs() {
        when(event.getKeyValuePairs()).thenReturn(Collections.emptyList());
        assertThat(supplier.map(event)).isEmpty();

    }

    @Test
    void multipleKeyValuePairs() {
        KeyValuePair pair1 = new KeyValuePair("key1", "value1");
        KeyValuePair pair2 = new KeyValuePair("key2", 123);
        when(event.getKeyValuePairs()).thenReturn(Arrays.asList(pair1, pair2));
        assertThat(supplier.map(event)).containsEntry("key1", "value1").containsEntry("key2", 123);
    }
}
