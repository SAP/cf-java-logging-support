package com.sap.cloud.cf.monitoring.spring.configuration.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sap.cloud.cf.monitoring.client.model.Metric;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.junit.jupiter.api.Test;

public abstract class MetricsConverterTest<T extends Meter> {

    private static final String STATISTIC_TAG = "statistic";
    protected static final String COUNT = "count";
    protected static final String MAX = "max";
    protected static final String MEAN = "mean";
    protected static final String TOTAL_TIME = "totalTime";
    protected static final Id ID = new Id("metricName", Arrays.asList(), "baseUnit", "description", Type.TIMER);

    protected final T meter;
    protected final MetricsConverter<T> converter;

    public MetricsConverterTest(T meter, MetricsConverter<T> converter) {
        this.meter = meter;
        this.converter = converter;
    }

    public abstract void assertResult(List<Metric> metrics);

    @Test
    public void testGetMetrics() {
        Stream<Metric> metricsStream = converter.getMetrics(meter);
        List<Metric> metrics = metricsStream.collect(Collectors.toList());

        assertResult(metrics);
    }

    @Test
    public void testToMetricWithNullValue() {
        Metric metric = converter.toMetric(ID, (0.0 / 0.0));

        assertNull(metric);
    }

    @Test
    public void getPercentileMetrics() {
        ValueAtPercentile[] percentiles = new ValueAtPercentile[] { new ValueAtPercentile(2.5, 2.0) };
        Stream<Metric> metricsStream = converter.getMetrics(meter, percentiles);
        List<Metric> metrics = metricsStream.collect(Collectors.toList());

        assertEquals(1, metrics.size());
        assertTrue(statisticTagExists(metrics, "250percentile"));
    }

    public T getMeter() {
        return meter;
    }

    protected boolean statisticTagExists(List<Metric> metrics, String type) {
        return metrics.stream().anyMatch(metric -> metric.getTags().get(STATISTIC_TAG).equals(type));
    }
}
