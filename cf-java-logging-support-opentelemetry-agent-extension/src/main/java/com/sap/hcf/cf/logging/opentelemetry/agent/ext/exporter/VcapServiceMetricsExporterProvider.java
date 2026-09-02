package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.VcapServiceProvider;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.EXPORTER;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static io.opentelemetry.sdk.metrics.Aggregation.explicitBucketHistogram;
import static java.lang.String.format;

public class VcapServiceMetricsExporterProvider implements ConfigurableMetricExporterProvider {

    private static final Logger LOG = Logger.getLogger(VcapServiceMetricsExporterProvider.class.getName());
    private static final String MSK_SKIP = " Skipping metrics exporter registration.";
    private static final String MSG_UNSUPPORTED_PROTOCOL =
            "Unsupported protocol \"%s\" for service binding \"%s\" (%s)." + MSK_SKIP;
    private static final String MSG_NO_BINDING =
            "No generic service binding found matching the configured criteria." + MSK_SKIP;
    private static final String MSG_NO_CREDENTIALS = "No credentials found for service binding \"%s\" (%s)." + MSK_SKIP;
    private static final String MSG_INVALID_CREDENTIALS =
            "Invalid credentials found for service binding \"%s\" (%s)." + MSK_SKIP;
    private static final String MSG_MISSING_SERVER_CERT =
            "No server certificate provided for service binding \"%s\" (%s). Attempting to download the server certificate from the endpoint.";
    private static final String MSG_FAIL_SERVER_CERT_DOWNLOAD =
            "Failed to download server certificate for service binding \"%s\" (%s)." + MSK_SKIP;

    private final Function<ConfigProperties, CloudFoundryServiceInstance> serviceProvider;
    private final Function<ConfigProperties, Function<CloudFoundryCredentials, VcapServiceCredentials>>
            credentialParserProvider;
    private final ServerCertificateDownloader serverCertificateDownloader;

    public VcapServiceMetricsExporterProvider() {
        this(config -> new VcapServiceProvider(config).get(),
             config -> VcapServiceCredentials.parser(config, RUNTIME.CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_METRICS_ENDPOINT),
             new ServerCertificateDownloader());
    }

    VcapServiceMetricsExporterProvider(
            Function<ConfigProperties, CloudFoundryServiceInstance> serviceProvider,
            Function<ConfigProperties, Function<CloudFoundryCredentials, VcapServiceCredentials>> credentialParserProvider,
            ServerCertificateDownloader serverCertificateDownloader) {
        this.serviceProvider = serviceProvider;
        this.credentialParserProvider = credentialParserProvider;
        this.serverCertificateDownloader = serverCertificateDownloader;
    }

    private static void addTimeout(Consumer<Duration> setter, ConfigProperties config) {
        Duration timeout = EXPORTER.VCAP_SERVICE.METRICS.TIMEOUT.getValue(config);
        if (timeout != null) {
            setter.accept(timeout);
        }
    }

    private static void addAuthTokenHeader(BiConsumer<String, String> setter, ConfigProperties config,
                                           VcapServiceCredentials credentials) {
        if (credentials.getAuthToken() != null) {
            String headerName = RUNTIME.CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.AUTH_HEADER_NAME.getValue(config);
            setter.accept(headerName, credentials.getAuthToken());
        }
    }

    private static AggregationTemporalitySelector getAggregationTemporalitySelector(ConfigProperties config) {
        String temporalityStr = EXPORTER.VCAP_SERVICE.METRICS.TEMPORALITY_PREFERENCE.getValue(config);
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
                EXPORTER.VCAP_SERVICE.METRICS.DEFAULT_HISTOGRAM_AGGREGATION.getValue(config);
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
        return "vcap-service";
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        CloudFoundryServiceInstance serviceInstance = serviceProvider.apply(config);
        if (serviceInstance == null) {
            LOG.info(MSG_NO_BINDING);
            return NoopMetricExporter.getInstance();
        }
        CloudFoundryCredentials rawCredentials = serviceInstance.getCredentials();
        if (rawCredentials == null) {
            LOG.warning(format(MSG_NO_CREDENTIALS, serviceInstance.getName(), serviceInstance.getLabel()));
            return NoopMetricExporter.getInstance();
        }
        VcapServiceCredentials credentials = credentialParserProvider.apply(config).apply(rawCredentials);
        if (credentials == null || !credentials.validate()) {
            LOG.warning(format(MSG_INVALID_CREDENTIALS, serviceInstance.getName(), serviceInstance.getLabel()));
            return NoopMetricExporter.getInstance();
        }

        MetricExporter exporter;
        switch (EXPORTER.VCAP_SERVICE.METRICS.PROTOCOL.getValue(config)) {
        case "http/protobuf":
            exporter = createHttpProtobufMetricExporter(config, credentials, serviceInstance);
            break;
        case "grpc":
            exporter = createGrpcMetricExporter(config, credentials, serviceInstance);
            break;
        default:
            LOG.warning(format(MSG_UNSUPPORTED_PROTOCOL, EXPORTER.VCAP_SERVICE.METRICS.PROTOCOL.getValue(config),
                               serviceInstance.getName(), serviceInstance.getLabel()));
            return NoopMetricExporter.getInstance();
        }

        if (exporter instanceof NoopMetricExporter) {
            return exporter;
        }
        return FilteringMetricExporter.wrap(exporter).withConfig(config)
                                      .withIncludedNames(EXPORTER.VCAP_SERVICE.METRICS.INCLUDE_NAMES)
                                      .withExcludedNames(EXPORTER.VCAP_SERVICE.METRICS.EXCLUDE_NAMES).build();
    }

    private MetricExporter createGrpcMetricExporter(ConfigProperties config, VcapServiceCredentials credentials,
                                                    CloudFoundryServiceInstance serviceInstance) {
        OtlpGrpcMetricExporterBuilder builder =
                OtlpGrpcMetricExporter.builder().setEndpoint(credentials.getEndpoint())
                                      .setCompression(EXPORTER.VCAP_SERVICE.METRICS.COMPRESSION.getValue(config))
                                      .setRetryPolicy(RetryPolicy.getDefault())
                                      .setAggregationTemporalitySelector(getAggregationTemporalitySelector(config))
                                      .setDefaultAggregationSelector(getDefaultAggregationSelector(config));
        addTimeout(builder::setTimeout, config);
        if (!addTlsConfig(builder::setClientTls, builder::setTrustedCertificates, credentials,
                          s -> format(s, serviceInstance.getName(), serviceInstance.getLabel()))) {
            return NoopMetricExporter.getInstance();
        }
        addAuthTokenHeader(builder::addHeader, config, credentials);
        return builder.build();
    }

    private MetricExporter createHttpProtobufMetricExporter(ConfigProperties config, VcapServiceCredentials credentials,
                                                            CloudFoundryServiceInstance serviceInstance) {
        OtlpHttpMetricExporterBuilder builder =
                OtlpHttpMetricExporter.builder().setEndpoint(credentials.getEndpoint())
                                      .setCompression(EXPORTER.VCAP_SERVICE.METRICS.COMPRESSION.getValue(config))
                                      .setRetryPolicy(RetryPolicy.getDefault())
                                      .setAggregationTemporalitySelector(getAggregationTemporalitySelector(config))
                                      .setDefaultAggregationSelector(getDefaultAggregationSelector(config));
        addTimeout(builder::setTimeout, config);
        if (!addTlsConfig(builder::setClientTls, builder::setTrustedCertificates, credentials,
                          s -> format(s, serviceInstance.getName(), serviceInstance.getLabel()))) {
            return NoopMetricExporter.getInstance();
        }
        addAuthTokenHeader(builder::addHeader, config, credentials);
        return builder.build();
    }

    private boolean addTlsConfig(BiConsumer<byte[], byte[]> clientTls, Consumer<byte[]> serverCert,
                                  VcapServiceCredentials credentials, Function<String, String> messageFormatter) {
        if (credentials.getClientKey() != null && credentials.getClientCert() != null) {
            clientTls.accept(credentials.getClientKey(), credentials.getClientCert());
            if (credentials.getServerCert() != null) {
                serverCert.accept(credentials.getServerCert());
            } else {
                LOG.info(messageFormatter.apply(MSG_MISSING_SERVER_CERT));
                String serverCertificate = serverCertificateDownloader.download(credentials.getEndpoint());
                if (serverCertificate != null) {
                    serverCert.accept(serverCertificate.getBytes(StandardCharsets.UTF_8));
                } else {
                    LOG.warning(messageFormatter.apply(MSG_FAIL_SERVER_CERT_DOWNLOAD));
                    return false;
                }
            }
        }
        return true;
    }
}
