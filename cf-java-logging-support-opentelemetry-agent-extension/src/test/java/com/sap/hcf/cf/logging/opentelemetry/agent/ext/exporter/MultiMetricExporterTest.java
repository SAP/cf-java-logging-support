package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class MultiMetricExporterTest {

    @Test
    public void createsNoopExporterOnNullExporterList() {
        assertThat(MultiMetricExporter.composite(emptyList(), null, null), is(instanceOf(NoopMetricExporter.class)));
    }

    @Test
    public void createsNoopExporterOnEmptyExporterList() {
        assertThat(MultiMetricExporter.composite(emptyList(), null, null), is(instanceOf(NoopMetricExporter.class)));
    }

    @Test
    public void returnsSingleExporterOnOneEntryExporterList() {
        MetricExporter exporter = mock(MetricExporter.class);
        assertThat(MultiMetricExporter.composite(singletonList(exporter), null, null), is(exporter));
    }

    @Test
    public void delegatesExport() {
        Collection<MetricData> metrics = Collections.emptyList();
        MetricExporter exporter1 = mock(MetricExporter.class);
        when(exporter1.export(metrics)).thenReturn(CompletableResultCode.ofSuccess());
        MetricExporter exporter2 = mock(MetricExporter.class);
        when(exporter2.export(metrics)).thenReturn(CompletableResultCode.ofSuccess());

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), null, null);
        metricExporter.export(metrics);

        verify(exporter1).export(metrics);
        verify(exporter2).export(metrics);
    }


    @Test
    public void delegatesFlush() {
        MetricExporter exporter1 = mock(MetricExporter.class);
        when(exporter1.flush()).thenReturn(CompletableResultCode.ofSuccess());
        MetricExporter exporter2 = mock(MetricExporter.class);
        when(exporter2.flush()).thenReturn(CompletableResultCode.ofSuccess());

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), null, null);
        metricExporter.flush();

        verify(exporter1).flush();
        verify(exporter2).flush();
    }


    @Test
    public void delegatesShutdwon() {
        MetricExporter exporter1 = mock(MetricExporter.class);
        when(exporter1.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
        MetricExporter exporter2 = mock(MetricExporter.class);
        when(exporter2.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), null, null);
        metricExporter.shutdown();

        verify(exporter1).shutdown();
        verify(exporter2).shutdown();
    }

    @Test
    public void delegatesAggregationTemporality() {
        MetricExporter exporter1 = mock(MetricExporter.class);
        MetricExporter exporter2 = mock(MetricExporter.class);

        AggregationTemporalitySelector aggregationTemporalitySelector = mock(AggregationTemporalitySelector.class);

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), aggregationTemporalitySelector, null);
        metricExporter.getAggregationTemporality(InstrumentType.OBSERVABLE_GAUGE);

        verify(aggregationTemporalitySelector).getAggregationTemporality(InstrumentType.OBSERVABLE_GAUGE);
    }

    @Test
    public void delegatesDefaultAggregation() {
        MetricExporter exporter1 = mock(MetricExporter.class);
        MetricExporter exporter2 = mock(MetricExporter.class);

        DefaultAggregationSelector defaultAggregationSelector = mock(DefaultAggregationSelector.class);

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), null, defaultAggregationSelector);
        metricExporter.getDefaultAggregation(InstrumentType.OBSERVABLE_GAUGE);

        verify(defaultAggregationSelector).getDefaultAggregation(InstrumentType.OBSERVABLE_GAUGE);
    }

    @Test
    public void delegatesAggregationTemporalityToFirstExporterIfNoExplicitAggregation() {
        MetricExporter exporter1 = mock(MetricExporter.class);
        MetricExporter exporter2 = mock(MetricExporter.class);

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), null, null);

        metricExporter.getAggregationTemporality(InstrumentType.OBSERVABLE_GAUGE);

        verify(exporter1).getAggregationTemporality(InstrumentType.OBSERVABLE_GAUGE);
    }

    @Test
    public void delegatesDefaultAggregationToFirstExporterIfNoExplicitAggregation() {
        MetricExporter exporter1 = mock(MetricExporter.class);
        MetricExporter exporter2 = mock(MetricExporter.class);

        MetricExporter metricExporter = MultiMetricExporter.composite(asList(exporter1, exporter2), null, null);

        metricExporter.getDefaultAggregation(InstrumentType.OBSERVABLE_GAUGE);

        verify(exporter1).getDefaultAggregation(InstrumentType.OBSERVABLE_GAUGE);
    }

}