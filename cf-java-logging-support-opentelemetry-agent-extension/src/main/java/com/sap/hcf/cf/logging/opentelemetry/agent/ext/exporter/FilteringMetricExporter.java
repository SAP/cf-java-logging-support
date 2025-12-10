package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ConfigProperty;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

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
        List<MetricData> filteredMetrics = collection.stream().filter(predicate).collect(toList());
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
        private ConfigProperty<List<String>> included;
        private ConfigProperty<List<String>> excluded;

        public Builder(MetricExporter delegate) {
            this.delegate = delegate;
        }

        public Builder withConfig(ConfigProperties config) {
            this.config = config;
            return this;
        }

        public Builder withIncludedNames(ConfigProperty<List<String>> included) {
            this.included = included;
            return this;
        }

        public Builder withExcludedNames(ConfigProperty<List<String>> excluded) {
            this.excluded = excluded;
            return this;
        }

        public MetricExporter build() {
            if (config == null) {
                return delegate;
            }

            List<String> includedNames = ofNullable(included).map(p -> p.getValue(config)).orElse(emptyList());
            List<String> excludedNames = ofNullable(excluded).map(p -> p.getValue(config)).orElse(emptyList());
            if (includedNames.isEmpty() && excludedNames.isEmpty()) {
                return delegate;
            }

            Predicate<MetricData> predicate = metricData -> true;
            predicate = addInclusions(predicate, includedNames);
            predicate = addExclusions(predicate, excludedNames);
            return new FilteringMetricExporter(delegate, predicate);
        }

        private static Predicate<MetricData> addInclusions(Predicate<MetricData> predicate,
                                                           List<String> includedNames) {
            if (includedNames.isEmpty()) {
                return predicate;
            }

            HashSet<String> names = getNames(includedNames);
            if (!names.isEmpty()) {
                predicate = predicate.and(metricData -> names.contains(metricData.getName()));
            }
            List<String> prefixes = getPrefixes(includedNames);
            if (!prefixes.isEmpty()) {
                predicate = predicate.and(
                        metricData -> prefixes.stream().anyMatch(p -> metricData.getName().startsWith(p)));
            }
            return predicate;
        }

        private static Predicate<MetricData> addExclusions(Predicate<MetricData> predicate,
                                                           List<String> excludedNames) {
            if (excludedNames.isEmpty()) {
                return predicate;
            }

            HashSet<String> names = getNames(excludedNames);
            if (!names.isEmpty()) {
                predicate = predicate.and(metricData -> !names.contains(metricData.getName()));
            }
            List<String> prefixes = getPrefixes(excludedNames);
            if (!prefixes.isEmpty()) {
                predicate = predicate.and(
                        metricData -> prefixes.stream().anyMatch(p -> !metricData.getName().startsWith(p)));
            }
            return predicate;
        }

        private static HashSet<String> getNames(List<String> names) {
            return names.stream().filter(n -> !n.endsWith("*")).collect(toCollection(HashSet::new));
        }

        private static List<String> getPrefixes(List<String> names) {
            return names.stream().filter(n -> n.endsWith("*")).map(n -> n.substring(0, n.length() - 1))
                        .collect(toList());
        }
    }
}
