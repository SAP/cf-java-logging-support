package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ConfigProperty;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties.createFromMap;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilteringMetricExporterTest {

    @Mock
    private MetricExporter delegate;

    @Mock(strictness = LENIENT)
    private MetricData includedMetric;

    @Mock(strictness = LENIENT)
    private MetricData excludedMetric;

    @Mock(strictness = LENIENT)
    private MetricData anotherMetric;

    @Mock
    private ConfigProperty<List<String>> includedMetricNames;

    @Mock
    private ConfigProperty<List<String>> excludedMetricNames;

    @Captor
    ArgumentCaptor<List<MetricData>> exported;

    @BeforeEach
    void setUp() {
        when(includedMetric.getName()).thenReturn("included");
        when(excludedMetric.getName()).thenReturn("excluded");
        when(anotherMetric.getName()).thenReturn("another");
    }

    private FilteringMetricExporter.Builder configureMetricExporter() {
        return FilteringMetricExporter.wrap(delegate).withIncludedNames(includedMetricNames)
                                      .withExcludedNames(excludedMetricNames);
    }

    @Test
    void exportsAllWithoutConfig() {
        try (MetricExporter exporter = configureMetricExporter().build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric, excludedMetric, anotherMetric);
    }

    @Test
    void exportsAllWithEmptyConfig() {
        DefaultConfigProperties config = createFromMap(emptyMap());
        when(includedMetricNames.getValue(config)).thenReturn(emptyList());
        when(excludedMetricNames.getValue(config)).thenReturn(emptyList());
        try (MetricExporter exporter = configureMetricExporter().withConfig(config).build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric, excludedMetric, anotherMetric);
    }

    @Test
    void exportsOnlyIncluded() {
        ConfigProperties config = createConfig(MapEntry.entry("include.names", "included"));
        when(includedMetricNames.getValue(config)).thenReturn(singletonList("included"));
        try (MetricExporter exporter = configureMetricExporter().withConfig(config).build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric);
    }

    @Test
    void rejectsExcluded() {
        ConfigProperties config = createConfig(MapEntry.entry("exclude.names", "excluded"));
        when(excludedMetricNames.getValue(config)).thenReturn(singletonList("excluded"));
        try (MetricExporter exporter = configureMetricExporter().withConfig(config).build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric, anotherMetric);
    }

    @Test
    void rejectsExcludedFromIncluded() {
        ConfigProperties config = createConfig(MapEntry.entry("include.names", "included,excluded"),
                                               MapEntry.entry("exclude.names", "excluded"));
        when(includedMetricNames.getValue(config)).thenReturn(asList("included", "excluded"));
        when(excludedMetricNames.getValue(config)).thenReturn(singletonList("excluded"));
        try (MetricExporter exporter = configureMetricExporter().withConfig(config).build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric);
    }

    @Test
    void supportsWildcardsOnIncluded() {
        ConfigProperties config = createConfig(MapEntry.entry("include.names", "incl*"));
        when(includedMetricNames.getValue(config)).thenReturn(singletonList("incl*"));
        try (MetricExporter exporter = configureMetricExporter().withConfig(config).build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric);
    }

    @Test
    void supportsWildcardsOnEncluded() {
        ConfigProperties config = createConfig(MapEntry.entry("exclude.names", "excl*"));
        when(excludedMetricNames.getValue(config)).thenReturn(singletonList("excl*"));
        try (MetricExporter exporter = configureMetricExporter().withConfig(config).build()) {
            exporter.export(asList(includedMetric, excludedMetric, anotherMetric));
        }
        verify(delegate).export(exported.capture());
        assertThat(exported.getValue()).containsExactlyInAnyOrder(includedMetric, anotherMetric);
    }

    @SafeVarargs
    private static ConfigProperties createConfig(MapEntry<String, String>... entries) {
        HashMap<String, String> map = new HashMap<>();
        for (MapEntry<String, String> entry: entries) {
            map.put(entry.key, entry.value);
        }
        return createFromMap(map);
    }

    @Test
    void close() {
        try (MetricExporter exporter = FilteringMetricExporter.wrap(delegate).build()) {
            // nothing to do
        }
        verify(delegate).close();
    }

    @Test
    void flush() {
        try (MetricExporter exporter = FilteringMetricExporter.wrap(delegate).build()) {
            exporter.flush();
        }
        verify(delegate).flush();
    }

    @Test
    void shutdown() {
        try (MetricExporter exporter = FilteringMetricExporter.wrap(delegate).build()) {
            exporter.shutdown();
        }
        verify(delegate).shutdown();
    }

    @Test
    void getAggregationTemporality() {
        when(delegate.getAggregationTemporality(InstrumentType.COUNTER)).thenReturn(AggregationTemporality.DELTA);
        try (MetricExporter exporter = FilteringMetricExporter.wrap(delegate).build()) {
            assertThat(exporter.getAggregationTemporality(InstrumentType.COUNTER)).isEqualTo(
                    AggregationTemporality.DELTA);
        }
    }

    @Test
    void getDefaultAggregation() {
        when(delegate.getDefaultAggregation(InstrumentType.COUNTER)).thenReturn(Aggregation.defaultAggregation());
        try (MetricExporter exporter = FilteringMetricExporter.wrap(delegate).build()) {
            assertThat(exporter.getDefaultAggregation(InstrumentType.COUNTER)).isEqualTo(
                    Aggregation.defaultAggregation());
        }
    }

    @Test
    void getMemoryMode() {
        when(delegate.getMemoryMode()).thenReturn(MemoryMode.IMMUTABLE_DATA);
        try (MetricExporter exporter = FilteringMetricExporter.wrap(delegate).build()) {
            assertThat(exporter.getMemoryMode()).isEqualTo(MemoryMode.IMMUTABLE_DATA);
        }
    }
}
