package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;

import java.util.Collections;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class KeyValuePairsFieldSupplier implements LogbackContextFieldSupplier {
    @Override
    public int order() {
        return LogbackContextFieldSupplier.CONTEXT_FIELDS + 1;
    }

    @Override
    public Map<String, Object> map(ILoggingEvent event) {
        if (event == null || event.getKeyValuePairs() == null) {
            return Collections.emptyMap();
        }
        return event.getKeyValuePairs().stream().collect(toMap(p -> p.key, p -> p.value));
    }
}
