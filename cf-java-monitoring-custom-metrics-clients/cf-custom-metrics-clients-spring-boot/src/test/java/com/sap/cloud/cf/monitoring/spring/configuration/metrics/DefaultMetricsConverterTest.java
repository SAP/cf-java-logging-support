package com.sap.cloud.cf.monitoring.spring.configuration.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.sap.cloud.cf.monitoring.client.model.Metric;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.internal.DefaultLongTaskTimer;

public class DefaultMetricsConverterTest extends MetricsConverterTest<Meter> {

    public DefaultMetricsConverterTest() {
        super(new DefaultLongTaskTimer(ID, Clock.SYSTEM), new DefaultMetricsConverter());
    }

    @Override
    public void assertResult(List<Metric> metrics) {
        assertEquals(2, metrics.size());
        assertTrue(statisticTagExists(metrics, "activeTasks"));
        assertTrue(statisticTagExists(metrics, "duration"));
    }
}
