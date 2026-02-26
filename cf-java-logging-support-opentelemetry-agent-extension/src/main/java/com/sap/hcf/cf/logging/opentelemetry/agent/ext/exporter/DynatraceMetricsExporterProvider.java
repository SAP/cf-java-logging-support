package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.DynatraceServiceProvider;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.EXPORTER;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.aggregator.AggregationUtil;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;

import static io.opentelemetry.sdk.metrics.Aggregation.explicitBucketHistogram;

public class DynatraceMetricsExporterProvider implements ConfigurableMetricExporterProvider {

    public static final String CRED_DYNATRACE_APIURL = "apiurl";
    public static final String DT_APIURL_METRICS_SUFFIX = "/v2/otlp/v1/metrics";

    private static final String GENERIC_CONFIG_PREFIX = "otel.exporter.dynatrace.";
    private static final String METRICS_CONFIG_PREFIX = "otel.exporter.dynatrace.metrics.";

    private static final Logger LOG = Logger.getLogger(DynatraceMetricsExporterProvider.class.getName());
    private static final AggregationTemporalitySelector ALWAYS_DELTA = instrumentType -> AggregationTemporality.DELTA;
    private final Function<ConfigProperties, CloudFoundryServiceInstance> serviceProvider;

    public DynatraceMetricsExporterProvider() {
        this(config -> new DynatraceServiceProvider(config).get());
    }

    public DynatraceMetricsExporterProvider(Function<ConfigProperties, CloudFoundryServiceInstance> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    private static String getCompression(ConfigProperties config) {
        return EXPORTER.DYNATRACE.METRICS.COMPRESSION.getValue(config);
    }

    private static Duration getTimeOut(ConfigProperties config) {
        return EXPORTER.DYNATRACE.METRICS.TIMEOUT.getValue(config);
    }

    private static AggregationTemporalitySelector getAggregationTemporalitySelector(ConfigProperties config) {
        String temporalityStr = EXPORTER.DYNATRACE.METRICS.TEMPORALITY_PREFERENCE.getValue(config);
        switch (temporalityStr.toLowerCase(Locale.ROOT)) {
        case "cumulative":
            return AggregationTemporalitySelector.alwaysCumulative();
        case "delta":
            return AggregationTemporalitySelector.deltaPreferred();
        case "lowmemory":
            return AggregationTemporalitySelector.lowMemory();
        case "always_delta":
            return ALWAYS_DELTA;
        default:
            throw new ConfigurationException("Unrecognized aggregation temporality: " + temporalityStr);
        }
    }

    private static DefaultAggregationSelector getDefaultAggregationSelector(ConfigProperties config) {
        String defaultHistogramAggregation = EXPORTER.DYNATRACE.METRICS.DEFAULT_HISTOGRAM_AGGREGATION.getValue(config);
        if (defaultHistogramAggregation == null) {
            return DefaultAggregationSelector.getDefault()
                                             .with(InstrumentType.HISTOGRAM, Aggregation.defaultAggregation());
        }
        if (AggregationUtil.aggregationName(Aggregation.base2ExponentialBucketHistogram())
                           .equalsIgnoreCase(defaultHistogramAggregation)) {
            return DefaultAggregationSelector.getDefault().with(InstrumentType.HISTOGRAM,
                                                                Aggregation.base2ExponentialBucketHistogram());
        } else if (AggregationUtil.aggregationName(explicitBucketHistogram())
                                  .equalsIgnoreCase(defaultHistogramAggregation)) {
            return DefaultAggregationSelector.getDefault()
                                             .with(InstrumentType.HISTOGRAM, Aggregation.explicitBucketHistogram());
        } else {
            throw new ConfigurationException(
                    "Unrecognized default histogram aggregation: " + defaultHistogramAggregation);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    @Override
    public String getName() {
        return "dynatrace";
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        CloudFoundryServiceInstance cfService = serviceProvider.apply(config);
        if (cfService == null) {
            LOG.info("No dynatrace service binding found. Skipping metrics exporter registration.");
            return NoopMetricExporter.getInstance();
        }

        LOG.info(
                "Creating metrics exporter for service binding " + cfService.getName() + " (" + cfService.getLabel() + ")");

        CloudFoundryCredentials credentials = cfService.getCredentials();
        if (credentials == null) {
            LOG.warning("No credentials found. Skipping dynatrace exporter configuration.");
            return NoopMetricExporter.getInstance();
        }
        String apiUrl = credentials.getString(CRED_DYNATRACE_APIURL);
        if (isBlank(apiUrl)) {
            LOG.warning(
                    "Credential \"" + CRED_DYNATRACE_APIURL + "\" not found. Skipping dynatrace exporter configuration");
            return NoopMetricExporter.getInstance();
        }
        String tokenName = RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TOKEN_NAME.getValue(config);
        if (isBlank(tokenName)) {
            LOG.warning(
                    "Configuration \"" + RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TOKEN_NAME.getKey() + "\" not found. Skipping dynatrace exporter configuration");
            return NoopMetricExporter.getInstance();
        }
        String apiToken = credentials.getString(tokenName);
        if (isBlank(apiUrl)) {
            LOG.warning("Credential \"" + tokenName + "\" not found. Skipping dynatrace exporter configuration");
            return NoopMetricExporter.getInstance();
        }

        OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder();
        builder.setEndpoint(apiUrl + DT_APIURL_METRICS_SUFFIX).setCompression(getCompression(config))
               .addHeader("Authorization", "Api-Token " + apiToken).setRetryPolicy(RetryPolicy.getDefault())
               .setAggregationTemporalitySelector(getAggregationTemporalitySelector(config))
               .setDefaultAggregationSelector(getDefaultAggregationSelector(config));

        Duration timeOut = getTimeOut(config);
        if (timeOut != null) {
            builder.setTimeout(timeOut);
        }

        LOG.info(
                "Created metrics exporter for service binding " + cfService.getName() + " (" + cfService.getLabel() + ")");
        MetricExporter exporter = builder.build();
        return FilteringMetricExporter.wrap(exporter).withConfig(config)
                                      .withIncludedNames(EXPORTER.DYNATRACE.METRICS.INCLUDE_NAMES)
                                      .withExcludedNames(EXPORTER.DYNATRACE.METRICS.EXCLUDE_NAMES).build();
    }

}
