package com.sap.cloud.cf.monitoring.spring.configuration.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.sap.cloud.cf.monitoring.client.model.Metric;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.noop.NoopTimer;

public class TimerMetricsConverterTest extends MetricsConverterTest<Timer> {

    public TimerMetricsConverterTest() {
        super(new NoopTimer(ID), new TimerMetricsConverter());
    }

    @Override
    public void assertResult(List<Metric> metrics) {
        assertEquals(4, metrics.size());
        assertTrue(statisticTagExists(metrics, COUNT));
        assertTrue(statisticTagExists(metrics, MAX));
        assertTrue(statisticTagExists(metrics, MEAN));
        assertTrue(statisticTagExists(metrics, TOTAL_TIME));
    }
}
