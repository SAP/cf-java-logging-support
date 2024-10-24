package com.sap.cloud.cf.monitoring.spring.configuration.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.sap.cloud.cf.monitoring.client.model.Metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.noop.NoopDistributionSummary;

public class DistributionSummaryMetricsConverterTest extends MetricsConverterTest<DistributionSummary> {

    public DistributionSummaryMetricsConverterTest() {
        super(new NoopDistributionSummary(ID), new DistributionSummaryMetricsConverter());
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
