package com.sap.hcp.cf.logging.sample.springboot.statistics;

import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsContextFieldSupplier implements ContextFieldSupplier {

    private static final String KEY = "message_count";

    private static final AtomicLong count = new AtomicLong();

    @Override
    public Map<String, Object> get() {
        return Map.of(KEY, count.incrementAndGet());
    }

}
