package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class MultiMetricExporter implements MetricExporter {

    private static final Logger LOG = Logger.getLogger(MultiMetricExporter.class.getName());

    private final AggregationTemporalitySelector aggregationTemporalitySelector;
    private final DefaultAggregationSelector defaultAggregationSelector;
    private final List<MetricExporter> metricExporters;

    private MultiMetricExporter(AggregationTemporalitySelector aggregationTemporalitySelector,
                                DefaultAggregationSelector defaultAggregationSelector,
                                List<MetricExporter> metricExporters) {
        this.aggregationTemporalitySelector = aggregationTemporalitySelector;
        this.defaultAggregationSelector = defaultAggregationSelector;
        this.metricExporters = metricExporters;
    }

    static MetricExporter composite(List<MetricExporter> metricExporters, AggregationTemporalitySelector aggregationTemporalitySelector, DefaultAggregationSelector defaultAggregationSelector) {
        if (metricExporters == null || metricExporters.isEmpty()) {
            return NoopMetricExporter.getInstance();
        }
        if (metricExporters.size() == 1) {
            return metricExporters.get(0);
        }
        if (aggregationTemporalitySelector == null) {
            aggregationTemporalitySelector = metricExporters.get(0);
        }
        if (defaultAggregationSelector == null) {
            defaultAggregationSelector = metricExporters.get(0);
        }
        return new MultiMetricExporter(aggregationTemporalitySelector, defaultAggregationSelector, metricExporters);
    }

    public CompletableResultCode export(Collection<MetricData> metrics) {
        List<CompletableResultCode> results = new ArrayList<>(metricExporters.size());
        for (MetricExporter metricExporter : metricExporters) {
            CompletableResultCode exportResult;
            try {
                exportResult = metricExporter.export(metrics);
                results.add(exportResult);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Exception thrown by the export.", e);
                results.add(CompletableResultCode.ofFailure());
            }
        }
        return CompletableResultCode.ofAll(results);
    }

    public CompletableResultCode flush() {
        List<CompletableResultCode> results = new ArrayList<>(this.metricExporters.size());
        for (MetricExporter metricExporter : metricExporters) {
            CompletableResultCode flushResult;
            try {
                flushResult = metricExporter.flush();
                results.add(flushResult);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Exception thrown by the flush.", e);
                results.add(CompletableResultCode.ofFailure());
            }
        }
        return CompletableResultCode.ofAll(results);
    }

    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = new ArrayList<>(this.metricExporters.size());
        for (MetricExporter metricExporter : metricExporters) {
            CompletableResultCode shutdownResult;
            try {
                shutdownResult = metricExporter.shutdown();
                results.add(shutdownResult);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Exception thrown by the shutdown.", e);
                results.add(CompletableResultCode.ofFailure());
            }
        }
        return CompletableResultCode.ofAll(results);
    }

    public String toString() {
        return "MultiMetricExporter"
                + metricExporters.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "{metricsExporters=", "}"));
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return aggregationTemporalitySelector.getAggregationTemporality(instrumentType);
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return defaultAggregationSelector.getDefaultAggregation(instrumentType);
    }

}