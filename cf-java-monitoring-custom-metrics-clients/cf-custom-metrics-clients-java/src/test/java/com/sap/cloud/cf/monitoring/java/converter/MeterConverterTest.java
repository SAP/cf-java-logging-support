package com.sap.cloud.cf.monitoring.java.converter;

import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_COUNT;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_M15_RATE;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_M1_RATE;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_M5_RATE;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_MEAN_RATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codahale.metrics.Meter;
import com.sap.cloud.cf.monitoring.client.model.Metric;
import com.sap.cloud.cf.monitoring.java.converter.MetricConverter.MetricType;

public class MeterConverterTest {
    private static final String METER_METRIC = "meterMetric";
    private static final Double METRIC_VALUE = 5.00;
    private long currentTimeMillis = System.currentTimeMillis();
    private SortedMap<String, Meter> meters;

    @BeforeEach
    public void setUp() throws Exception {
        meters = new TreeMap<>();
        Meter meter = new Meter();
        meters.put(METER_METRIC, meter);
        meter.mark(METRIC_VALUE.longValue());
    }

    @Test
    public void testMeterMetricsWithMetricQuantiles() {
        List<Metric> metrics = new MeterConverter(true).convert(meters, currentTimeMillis);

        ConverterTestUtil util =
            new ConverterTestUtil(metrics, METER_METRIC, MetricType.METER.getMetricTypeName(), currentTimeMillis);
        util.checkMetric(SUFFIX_COUNT, METRIC_VALUE);
        util.checkMetric(SUFFIX_M1_RATE);
        util.checkMetric(SUFFIX_M5_RATE);
        util.checkMetric(SUFFIX_M15_RATE);
        util.checkMetric(SUFFIX_MEAN_RATE);
        assertEquals(5, metrics.size());
    }

    @Test
    public void testMeterMetricsWithoutMetricQuantiles() {
        List<Metric> metrics = new MeterConverter(false).convert(meters, currentTimeMillis);

        ConverterTestUtil util =
            new ConverterTestUtil(metrics, METER_METRIC, MetricType.METER.getMetricTypeName(), currentTimeMillis);

        util.checkMetric(SUFFIX_COUNT, METRIC_VALUE);
        util.checkMetric(SUFFIX_M1_RATE);

        assertEquals(2, metrics.size());
    }
}