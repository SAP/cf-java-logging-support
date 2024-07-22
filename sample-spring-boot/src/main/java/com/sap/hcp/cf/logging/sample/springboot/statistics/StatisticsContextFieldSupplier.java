package com.sap.hcp.cf.logging.sample.springboot.statistics;

import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsContextFieldSupplier implements ContextFieldSupplier {

    private static final String KEY = "message_count";

    private static AtomicLong count = new AtomicLong();

    @SuppressWarnings("serial")
    @Override
    public Map<String, Object> get() {
        return new HashMap<String, Object>() {
            {
                put(KEY, count.incrementAndGet());
            }
        };
    }

}
