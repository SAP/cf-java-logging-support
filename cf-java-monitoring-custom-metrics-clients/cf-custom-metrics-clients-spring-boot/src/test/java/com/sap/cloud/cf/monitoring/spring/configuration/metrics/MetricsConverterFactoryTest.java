package com.sap.cloud.cf.monitoring.spring.configuration.metrics;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sap.cloud.cf.monitoring.client.model.Metric;

import org.junit.jupiter.api.Test;

public class MetricsConverterFactoryTest {

    @Test
    public void testGetMetrics_withDefaultMetricsConverter() {
        testGetMetrics(new DefaultMetricsConverterTest());
    }

    @Test
    public void testGetMetrics_withDistributionSummaryMetricsConverter() {
        testGetMetrics(new DistributionSummaryMetricsConverterTest());
    }

    @Test
    public void testGetMetrics_withFunctionTimerMetricsConverter() {
        testGetMetrics(new FunctionTimerMetricsConverterTest());
    }

    @Test
    public void testGetMetrics_withTimerMetricsConverter() {
        testGetMetrics(new TimerMetricsConverterTest());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void testGetMetrics(MetricsConverterTest converterTest) {
        Stream<Metric> metricsStream = MetricsConverterFactory.getMetrics(converterTest.getMeter());
        List<Metric> metrics = metricsStream.collect(Collectors.toList());

        converterTest.assertResult(metrics);
    }
}
