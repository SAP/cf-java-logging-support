package com.sap.cloud.cf.monitoring.spring.configuration.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.sap.cloud.cf.monitoring.client.model.Metric;

import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.noop.NoopFunctionTimer;

public class FunctionTimerMetricsConverterTest extends MetricsConverterTest<FunctionTimer> {

    public FunctionTimerMetricsConverterTest() {
        super(new NoopFunctionTimer(ID), new FunctionTimerMetricsConverter());
    }

    @Override
    public void assertResult(List<Metric> metrics) {
        assertEquals(3, metrics.size());
        assertTrue(statisticTagExists(metrics, COUNT));
        assertTrue(statisticTagExists(metrics, MEAN));
        assertTrue(statisticTagExists(metrics, TOTAL_TIME));
    }
}
