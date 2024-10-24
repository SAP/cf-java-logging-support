package com.sap.cloud.cf.monitoring.java.converter;

import static com.sap.cloud.cf.monitoring.java.converter.ConverterTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.sap.cloud.cf.monitoring.client.model.Metric;
import com.sap.cloud.cf.monitoring.java.converter.MetricConverter.MetricType;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
public class TimerConverterTest {

    private static final String TIMER_METRIC = "timerMetric";
    private final long currentTimeMillis = System.currentTimeMillis();
    private SortedMap<String, Timer> timers;

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

        timers = new TreeMap<>();
        Timer timer = new Timer(reservoir);
        timers.put(TIMER_METRIC, timer);

    }

    @Test
    public void testTimerMetricWithMetricQuantiles() {
        List<Metric> metrics = new TimerConverter(true).convert(timers, currentTimeMillis);
        ConverterTestUtil util =
            new ConverterTestUtil(metrics, TIMER_METRIC, MetricType.TIMER.getMetricTypeName(), currentTimeMillis);
        util.checkMetric(SUFFIX_COUNT, 0d);
        util.checkMetric(SUFFIX_M1_RATE, null);
        util.checkMetric(SUFFIX_M5_RATE, null);
        util.checkMetric(SUFFIX_M15_RATE, null);
        util.checkMetric(SUFFIX_MEAN_RATE, null);
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
        assertEquals(15, metrics.size());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testTimerMetricWithoutMetricQuantiles() {
        List<Metric> metrics = new TimerConverter(false).convert(timers, currentTimeMillis);

        ConverterTestUtil util =
            new ConverterTestUtil(metrics, TIMER_METRIC, MetricType.TIMER.getMetricTypeName(), currentTimeMillis);

        util.checkMetric(SUFFIX_COUNT, 0d);
        util.checkMetric(SUFFIX_MAX, 100d);
        util.checkMetric(SUFFIX_MIN, 99d);
        util.checkMetric(SUFFIX_P50, 50d);
        util.checkMetric(SUFFIX_P95, 95d);
        util.checkMetric(SUFFIX_P99, 99d);
        util.checkMetric(SUFFIX_M1_RATE, null);

        assertEquals(7, metrics.size());
    }
}
