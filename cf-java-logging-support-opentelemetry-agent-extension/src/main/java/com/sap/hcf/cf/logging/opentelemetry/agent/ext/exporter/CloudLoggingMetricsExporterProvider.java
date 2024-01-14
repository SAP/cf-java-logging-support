package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

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
import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.opentelemetry.sdk.metrics.Aggregation.explicitBucketHistogram;

public class CloudLoggingMetricsExporterProvider implements ConfigurableMetricExporterProvider {

    private static final Logger LOG = Logger.getLogger(CloudLoggingMetricsExporterProvider.class.getName());

    private final Function<ConfigProperties, Stream<CfService>> servicesProvider;
    private final CloudLoggingCredentials.Parser credentialParser;

    public CloudLoggingMetricsExporterProvider() {
        this(config -> new CloudLoggingServicesProvider(config).get(), CloudLoggingCredentials.parser());
    }

    CloudLoggingMetricsExporterProvider(Function<ConfigProperties, Stream<CfService>> serviceProvider, CloudLoggingCredentials.Parser credentialParser) {
        this.servicesProvider = serviceProvider;
        this.credentialParser = credentialParser;
    }

    private static String getCompression(ConfigProperties config) {
        String compression = config.getString("otel.exporter.cloud-logging.metrics.compression");
        return compression != null ? compression : config.getString("otel.exporter.cloud-logging.compression", "gzip");
    }

    private static Duration getTimeOut(ConfigProperties config) {
        Duration timeout = config.getDuration("otel.exporter.cloud-logging.metrics.timeout");
        return timeout != null ? timeout : config.getDuration("otel.exporter.cloud-logging.timeout");
    }

    private static AggregationTemporalitySelector getAggregationTemporalitySelector(ConfigProperties config) {
        String temporalityStr = config.getString("otel.exporter.cloud-logging.metrics.temporality.preference");
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
        String defaultHistogramAggregation =
                config.getString("otel.exporter.cloud-logging.metrics.default.histogram.aggregation");
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
        return "cloud-logging";
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        List<MetricExporter> exporters = servicesProvider.apply(config)
                .map(svc -> createExporter(config, svc))
                .filter(exp -> !(exp instanceof NoopMetricExporter))
                .collect(Collectors.toList());
        return MultiMetricExporter.composite(exporters, getAggregationTemporalitySelector(config), getDefaultAggregationSelector(config));
    }

    private MetricExporter createExporter(ConfigProperties config, CfService service) {
        LOG.info("Creating metrics exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        CfCredentials cfCredentials = service.getCredentials();
        CloudLoggingCredentials credentials = credentialParser.parse(cfCredentials);
        if (!credentials.validate()) {
            return NoopMetricExporter.getInstance();
        }

        OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder();
        builder.setEndpoint(credentials.getEndpoint())
                .setCompression(getCompression(config))
                .setClientTls(credentials.getClientKey(), credentials.getClientCert())
                .setTrustedCertificates(credentials.getServerCert())
                .setRetryPolicy(RetryPolicy.getDefault())
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
