package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.aggregator.AggregationUtil;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;

import static io.opentelemetry.sdk.metrics.Aggregation.explicitBucketHistogram;

public class DynatraceMetricsExporterProvider implements ConfigurableMetricExporterProvider {

    private static final Logger LOG = Logger.getLogger(DynatraceMetricsExporterProvider.class.getName());
    public static final String CRED_DYNATRACE_APIURL = "apiurl";
    public static final String DT_APIURL_METRICS_SUFFIX = "/v2/otlp/v1/metrics";

    private final Function<ConfigProperties, CfService> serviceProvider;

    public DynatraceMetricsExporterProvider() {
        this(config -> new DynatraceServiceProvider(config, new CfEnv()).get());
    }

    public DynatraceMetricsExporterProvider(Function<ConfigProperties, CfService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    private static String getCompression(ConfigProperties config) {
        String compression = config.getString("otel.exporter.dynatrace.metrics.compression");
        return compression != null ? compression : config.getString("otel.exporter.dynatrace.compression", "gzip");
    }

    private static Duration getTimeOut(ConfigProperties config) {
        Duration timeout = config.getDuration("otel.exporter.dynatrace.metrics.timeout");
        return timeout != null ? timeout : config.getDuration("otel.exporter.dynatrace.timeout");
    }

    private static DefaultAggregationSelector getDefaultAggregationSelector(ConfigProperties config) {
        String defaultHistogramAggregation =
                config.getString("otel.exporter.dynatrace.metrics.default.histogram.aggregation");
        if (defaultHistogramAggregation == null) {
            return DefaultAggregationSelector.getDefault().with(InstrumentType.HISTOGRAM, Aggregation.defaultAggregation());
        }
        if (AggregationUtil.aggregationName(Aggregation.base2ExponentialBucketHistogram())
                .equalsIgnoreCase(defaultHistogramAggregation)) {
            return
                    DefaultAggregationSelector.getDefault()
                            .with(InstrumentType.HISTOGRAM, Aggregation.base2ExponentialBucketHistogram());
        } else if (AggregationUtil.aggregationName(explicitBucketHistogram())
                .equalsIgnoreCase(defaultHistogramAggregation)) {
            return DefaultAggregationSelector.getDefault().with(InstrumentType.HISTOGRAM, Aggregation.explicitBucketHistogram());
        } else {
            throw new ConfigurationException(
                    "Unrecognized default histogram aggregation: " + defaultHistogramAggregation);
        }
    }

    @Override
    public String getName() {
        return "dynatrace";
    }


    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        CfService cfService = serviceProvider.apply(config);
        if (cfService == null) {
            LOG.info("No dynatrace service binding found. Skipping metrics exporter registration.");
            return NoopMetricExporter.getInstance();
        }

        LOG.info("Creating metrics exporter for service binding " + cfService.getName() + " (" + cfService.getLabel() + ")");

        String apiUrl = cfService.getCredentials().getString(CRED_DYNATRACE_APIURL);
        if (isBlank(apiUrl)) {
            LOG.warning("Credential \"" + CRED_DYNATRACE_APIURL + "\" not found. Skipping dynatrace exporter configuration");
            return NoopMetricExporter.getInstance();
        }
        String tokenName = config.getString("otel.javaagent.extension.sap.cf.binding.dynatrace.metrics.token-name");
        if (isBlank(tokenName)) {
            LOG.warning("Configuration \"otel.javaagent.extension.sap.cf.binding.dynatrace.metrics.token-name\" not found. Skipping dynatrace exporter configuration");
            return NoopMetricExporter.getInstance();
        }
        String apiToken = cfService.getCredentials().getString(tokenName);
        if (isBlank(apiUrl)) {
            LOG.warning("Credential \"" + tokenName + "\" not found. Skipping dynatrace exporter configuration");
            return NoopMetricExporter.getInstance();
        }

        OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder();
        builder.setEndpoint(apiUrl + DT_APIURL_METRICS_SUFFIX)
                .setCompression(getCompression(config))
                .addHeader("Authorization", "ApiToken " + apiToken)
                .setRetryPolicy(RetryPolicy.getDefault())
                .setAggregationTemporalitySelector(AggregationTemporalitySelector.alwaysCumulative())
                .setDefaultAggregationSelector(getDefaultAggregationSelector(config));

        Duration timeOut = getTimeOut(config);
        if (timeOut != null) {
            builder.setTimeout(timeOut);
        }

        LOG.info("Created metrics exporter for service binding " + cfService.getName() + " (" + cfService.getLabel() + ")");
        return builder.build();
    }

}
