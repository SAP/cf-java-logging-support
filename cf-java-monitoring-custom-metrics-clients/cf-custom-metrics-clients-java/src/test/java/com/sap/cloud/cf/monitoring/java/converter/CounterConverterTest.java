package com.sap.cloud.cf.monitoring.java.converter;

import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codahale.metrics.Counter;
import com.sap.cloud.cf.monitoring.client.model.Metric;
import com.sap.cloud.cf.monitoring.java.converter.CounterConverter;
import com.sap.cloud.cf.monitoring.java.converter.MetricConverter.MetricType;

public class CounterConverterTest {

    private static final String COUNTER_METRIC = "counterMetric";
    private static final Double METRIC_VALUE = 5.00;
    private final long currentTimeMillis = System.currentTimeMillis();
    private List<Metric> metrics;

    private CounterConverter counterConverter;

    @BeforeEach
    public void setUp() throws Exception {
        counterConverter = new CounterConverter();
        SortedMap<String, Counter> counters = new TreeMap<>();
        Counter counter = new Counter();
        counters.put(COUNTER_METRIC, counter);
        counter.inc(METRIC_VALUE.intValue());
        metrics = counterConverter.convert(counters, currentTimeMillis);
    }

    @Test
    public void testCounterMetric() {
        ConverterTestUtil util =
            new ConverterTestUtil(metrics, COUNTER_METRIC, MetricType.COUNTER.getMetricTypeName(), currentTimeMillis);
        util.checkMetric(SUFFIX_COUNT, METRIC_VALUE);
        assertEquals(1, metrics.size());
    }
}