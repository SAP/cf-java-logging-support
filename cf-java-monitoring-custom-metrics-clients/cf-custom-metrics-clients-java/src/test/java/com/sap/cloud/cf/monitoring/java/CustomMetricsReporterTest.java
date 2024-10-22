package com.sap.cloud.cf.monitoring.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.sap.cloud.cf.monitoring.client.MonitoringClient;
import com.sap.cloud.cf.monitoring.client.configuration.CustomMetricsConfiguration;
import com.sap.cloud.cf.monitoring.client.exceptions.MonitoringClientException;
import com.sap.cloud.cf.monitoring.client.model.Metric;

@RunWith(MockitoJUnitRunner.class)
public class CustomMetricsReporterTest {

    private static final String METRIC_NAME = "metricName";
    private MetricRegistry registry;
    private CustomMetricsReporter reporter;

    @Mock
    private MonitoringClient client;

    @Mock
    private CustomMetricsConfiguration customMetricsConfig;

    @Before
    public void setup() {
        registry = new MetricRegistry();

        reporter = new CustomMetricsReporter(registry, client, customMetricsConfig);
    }

    @Test
    public void testReportCounterSuccessfully() {
        Counter counter = registry.counter(METRIC_NAME);
        counter.inc(3);

        reporter.report();

        checkMetricsAreSent(1, "counter", METRIC_NAME);
    }

    @Test
    public void testReportGaugeSuccessfully() {
        registry.register(METRIC_NAME, new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return 13;
            }
        });

        reporter.report();

        checkMetricsAreSent(1, "gauge", METRIC_NAME);
    }

    @Test
    public void testReportHistogramWithMetricQuantilesSuccessfully() {
        when(customMetricsConfig.metricQuantiles()).thenReturn(true);
        registry.histogram(METRIC_NAME);

        reporter.report();

        checkMetricsAreSent(11, "histogram", METRIC_NAME);
    }

    @Test
    public void testReportHistogramWithoutMetricQuantilesSuccessfully() {
        when(customMetricsConfig.metricQuantiles()).thenReturn(false);
        registry.histogram(METRIC_NAME);

        reporter.report();

        checkMetricsAreSent(6, "histogram", METRIC_NAME);
    }

    @Test
    public void testReportMeterWithMetricQuantilesSuccessfully() {
        when(customMetricsConfig.metricQuantiles()).thenReturn(true);
        registry.meter(METRIC_NAME);

        reporter.report();

        checkMetricsAreSent(5, "meter", METRIC_NAME);
    }

    @Test
    public void testReportMeterWithoutMetricQuantilesSuccessfully() {
        when(customMetricsConfig.metricQuantiles()).thenReturn(false);
        registry.meter(METRIC_NAME);

        reporter.report();

        checkMetricsAreSent(2, "meter", METRIC_NAME);
    }

    @Test
    public void testReportEmptyMetrics() {
        reporter.report();

        verify(client, never()).send(anyList());
    }

    @Test
    public void testReportMetricWithWhitelist() {
        when(customMetricsConfig.metricQuantiles()).thenReturn(true);
        when(customMetricsConfig.getMetrics()).thenReturn(Arrays.asList(METRIC_NAME));

        registry.meter(METRIC_NAME);
        reporter = new CustomMetricsReporter(registry, client, customMetricsConfig);
        reporter.report();

        checkMetricsAreSent(5, "meter", METRIC_NAME);
    }

    @Test
    public void testReportMetricWithoutWhitelist() {
        when(customMetricsConfig.getMetrics()).thenReturn(Arrays.asList("whitelistMetricName"));

        registry.meter(METRIC_NAME);
        reporter = new CustomMetricsReporter(registry, client, customMetricsConfig);
        reporter.report();

        verify(client, never()).send(anyList());
    }

    @Test
    public void testReportNonEmptyMetricsWithMonitoringClientEx() {
        doThrow(MonitoringClientException.class).when(client).send(anyList());
        registry.counter(METRIC_NAME);

        reporter.report();

        verify(client, times(CustomMetricsReporter.SEND_METRICS_ATTEMPTS)).send(anyList());
    }

    @Test
    public void testReportNonEmptyMetricsWithException() {
        doThrow(Exception.class).when(client).send(anyList());
        registry.counter(METRIC_NAME);

        reporter.report();

        verify(client, times(1)).send(anyList());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void checkMetricsAreSent(int metricsCount, String type, String name) {
        Class<ArrayList<Metric>> listClass = (Class) ArrayList.class;
        ArgumentCaptor<ArrayList<Metric>> argument = ArgumentCaptor.forClass(listClass);

        verify(client).send(argument.capture());
        assertEquals(metricsCount, argument.getValue().size());
        for (Metric metric : argument.getValue()) {
            assertEquals(type, metric.getTag("type"));
            assertTrue(metric.getName().contains(name));
        }
    }
}