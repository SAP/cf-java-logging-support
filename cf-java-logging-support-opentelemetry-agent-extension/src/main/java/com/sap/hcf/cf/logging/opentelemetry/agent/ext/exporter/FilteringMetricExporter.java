package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilteringMetricExporter implements MetricExporter {

    private static final String INCLUDED_NAMES_KEY = "include.names";
    private static final String EXCLUDED_NAMES_KEY = "exclude.names";

    private final MetricExporter delegate;
    private final Predicate<MetricData> predicate;

    private FilteringMetricExporter(MetricExporter delegate, Predicate<MetricData> predicate) {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> collection) {
        List<MetricData> filteredMetrics = collection.stream().filter(predicate).collect(Collectors.toList());
        return delegate.export(filteredMetrics);
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return delegate.getAggregationTemporality(instrumentType);
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return delegate.getDefaultAggregation(instrumentType);
    }

    @Override
    public MemoryMode getMemoryMode() {
        return delegate.getMemoryMode();
    }

    public static Builder wrap(MetricExporter delegate) {
        return new Builder(delegate);
    }

    public static class Builder {

        private final MetricExporter delegate;
        private ConfigProperties config;
        private String prefix = "";

        public Builder(MetricExporter delegate) {
            this.delegate = delegate;
        }

        public Builder withConfig(ConfigProperties config) {
            this.config = config;
            return this;
        }

        public Builder withPropertyPrefix(String prefix) {
            this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
            return this;
        }

        public MetricExporter build() {
            if (config == null) {
                return delegate;
            }

            List<String> includedNames = config.getList(prefix + INCLUDED_NAMES_KEY);
            List<String> excludedNames = config.getList(prefix + EXCLUDED_NAMES_KEY);
            if (includedNames.isEmpty() && excludedNames.isEmpty()) {
                return delegate;
            }

            Predicate<MetricData> predicate = metricData -> true;
            if (!includedNames.isEmpty()) {
                predicate = predicate.and(metricData -> includedNames.contains(metricData.getName()));
            }
            if (!excludedNames.isEmpty()) {
                predicate = predicate.and(metricData -> !excludedNames.contains(metricData.getName()));
            }
            return new FilteringMetricExporter(delegate, predicate);
        }
    }
}
