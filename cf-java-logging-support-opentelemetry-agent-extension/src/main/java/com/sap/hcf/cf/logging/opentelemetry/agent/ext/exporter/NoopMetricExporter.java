package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collection;

public class NoopMetricExporter implements MetricExporter {

    private static final MetricExporter INSTANCE = new NoopMetricExporter();

    NoopMetricExporter() {
    }

    static MetricExporter getInstance() {
        return INSTANCE;
    }


    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporalitySelector.alwaysCumulative().getAggregationTemporality(instrumentType);
    }
}
