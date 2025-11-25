package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingServicesProvider;
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

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.opentelemetry.sdk.metrics.Aggregation.explicitBucketHistogram;

public class CloudLoggingMetricsExporterProvider implements ConfigurableMetricExporterProvider {

    private static final String GENERIC_CONFIG_PREFIX = "otel.exporter.cloud-logging.";
    private static final String METRICS_CONFIG_PREFIX = "otel.exporter.cloud-logging.metrics.";
    private static final Logger LOG = Logger.getLogger(CloudLoggingMetricsExporterProvider.class.getName());

    private final Function<ConfigProperties, Stream<CloudFoundryServiceInstance>> servicesProvider;
    private final CloudLoggingCredentials.Parser credentialParser;

    public CloudLoggingMetricsExporterProvider() {
        this(config -> new CloudLoggingServicesProvider(config).get(), CloudLoggingCredentials.parser());
    }

    CloudLoggingMetricsExporterProvider(Function<ConfigProperties, Stream<CloudFoundryServiceInstance>> serviceProvider,
                                        CloudLoggingCredentials.Parser credentialParser) {
        this.servicesProvider = serviceProvider;
        this.credentialParser = credentialParser;
    }

    private static String getCompression(ConfigProperties config) {
        String compression = config.getString(METRICS_CONFIG_PREFIX + "compression");
        return compression != null ? compression : config.getString(GENERIC_CONFIG_PREFIX + "compression", "gzip");
    }

    private static Duration getTimeOut(ConfigProperties config) {
        Duration timeout = config.getDuration(METRICS_CONFIG_PREFIX + "timeout");
        return timeout != null ? timeout : config.getDuration(GENERIC_CONFIG_PREFIX + "timeout");
    }

    private static AggregationTemporalitySelector getAggregationTemporalitySelector(ConfigProperties config) {
        String temporalityStr = config.getString(METRICS_CONFIG_PREFIX + "temporality.preference");
        if (temporalityStr == null) {
            return AggregationTemporalitySelector.alwaysCumulative();
        }
        AggregationTemporalitySelector temporalitySelector;
        switch (temporalityStr.toLowerCase(Locale.ROOT)) {
        case "cumulative":
            return AggregationTemporalitySelector.alwaysCumulative();
        case "delta":
            return AggregationTemporalitySelector.deltaPreferred();
        case "lowmemory":
            return AggregationTemporalitySelector.lowMemory();
        default:
            throw new ConfigurationException("Unrecognized aggregation temporality: " + temporalityStr);
        }
    }

    private static DefaultAggregationSelector getDefaultAggregationSelector(ConfigProperties config) {
        String defaultHistogramAggregation = config.getString(METRICS_CONFIG_PREFIX + "default.histogram.aggregation");
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

    @Override
    public String getName() {
        return "cloud-logging";
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        List<MetricExporter> exporters = servicesProvider.apply(config).map(svc -> createExporter(config, svc))
                                                         .filter(exp -> !(exp instanceof NoopMetricExporter))
                                                         .collect(Collectors.toList());
        MetricExporter exporter = MultiMetricExporter.composite(exporters, getAggregationTemporalitySelector(config),
                                                                getDefaultAggregationSelector(config));
        exporter = FilteringMetricExporter.wrap(exporter).withConfig(config).withPropertyPrefix(METRICS_CONFIG_PREFIX)
                                          .build();
        return exporter;
    }

    private MetricExporter createExporter(ConfigProperties config, CloudFoundryServiceInstance service) {
        LOG.info(
                "Creating metrics exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        CloudLoggingCredentials credentials = credentialParser.parse(service.getCredentials());
        if (!credentials.validate()) {
            return NoopMetricExporter.getInstance();
        }

        OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder();
        builder.setEndpoint(credentials.getEndpoint()).setCompression(getCompression(config))
               .setClientTls(credentials.getClientKey(), credentials.getClientCert())
               .setTrustedCertificates(credentials.getServerCert()).setRetryPolicy(RetryPolicy.getDefault())
               .setAggregationTemporalitySelector(getAggregationTemporalitySelector(config))
               .setDefaultAggregationSelector(getDefaultAggregationSelector(config));

        Duration timeOut = getTimeOut(config);
        if (timeOut != null) {
            builder.setTimeout(timeOut);
        }

        LOG.info("Created metrics exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        return builder.build();
    }
}
