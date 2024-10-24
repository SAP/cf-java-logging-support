package com.sap.cloud.cf.monitoring.java.converter;

import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_COUNT;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_MAX;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_MEAN;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_MIN;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_P50;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_P75;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_P95;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_P98;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_P99;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_P999;
import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.SUFFIX_STDDEV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.sap.cloud.cf.monitoring.client.model.Metric;
import com.sap.cloud.cf.monitoring.java.converter.MetricConverter.MetricType;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
public class HistogramConverterTest {

    private static final String HISTOGRAM_METRIC = "histogramMetric";
    private final long currentTimeMillis = System.currentTimeMillis();
    private Map<String, Histogram> histograms;

    @Mock
    private Reservoir reservoir;

    @Mock
    private Snapshot snapshot;

    @BeforeEach
    public void setUp() throws Exception {
        when(reservoir.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMax()).thenReturn(100l);
        when(snapshot.getMin()).thenReturn(99l);
        when(snapshot.getMean()).thenReturn(12d);
        when(snapshot.getMedian()).thenReturn(50d);
        when(snapshot.get75thPercentile()).thenReturn(75d);
        when(snapshot.get95thPercentile()).thenReturn(95d);
        when(snapshot.get98thPercentile()).thenReturn(98d);
        when(snapshot.get99thPercentile()).thenReturn(99d);
        when(snapshot.get999thPercentile()).thenReturn(999d);
        when(snapshot.getStdDev()).thenReturn(9d);

        histograms = new TreeMap<>();
        Histogram histogram = new Histogram(reservoir);
        histograms.put(HISTOGRAM_METRIC, histogram);

    }

    @Test
    public void testTimerMetricWithMetricQuantiles() {
        List<Metric> metrics = new HistogramConverter(true).convert(histograms, currentTimeMillis);

        ConverterTestUtil util = new ConverterTestUtil(metrics, HISTOGRAM_METRIC,
                MetricType.HISTOGRAM.getMetricTypeName(), currentTimeMillis);
        util.checkMetric(SUFFIX_COUNT, 0d);
        util.checkMetric(SUFFIX_MAX, 100d);
        util.checkMetric(SUFFIX_MEAN, 12d);
        util.checkMetric(SUFFIX_MIN, 99d);
        util.checkMetric(SUFFIX_P50, 50d);
        util.checkMetric(SUFFIX_P75, 75d);
        util.checkMetric(SUFFIX_P95, 95d);
        util.checkMetric(SUFFIX_P98, 98d);
        util.checkMetric(SUFFIX_P99, 99d);
        util.checkMetric(SUFFIX_P999, 999d);
        util.checkMetric(SUFFIX_STDDEV, 9d);
        assertEquals(11, metrics.size());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testTimerMetricWithoutMetricQuantiles() {
        List<Metric> metrics = new HistogramConverter(false).convert(histograms, currentTimeMillis);

        ConverterTestUtil util = new ConverterTestUtil(metrics, HISTOGRAM_METRIC,
                MetricType.HISTOGRAM.getMetricTypeName(), currentTimeMillis);
        util.checkMetric(SUFFIX_COUNT, 0d);
        util.checkMetric(SUFFIX_MAX, 100d);
        util.checkMetric(SUFFIX_MIN, 99d);
        util.checkMetric(SUFFIX_P50, 50d);
        util.checkMetric(SUFFIX_P95, 95d);
        util.checkMetric(SUFFIX_P99, 99d);

        assertEquals(6, metrics.size());
    }
}