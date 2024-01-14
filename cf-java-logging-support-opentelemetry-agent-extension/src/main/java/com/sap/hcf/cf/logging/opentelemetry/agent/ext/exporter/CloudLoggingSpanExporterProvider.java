package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingServicesProvider;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudLoggingSpanExporterProvider implements ConfigurableSpanExporterProvider {

    private static final Logger LOG = Logger.getLogger(CloudLoggingSpanExporterProvider.class.getName());

    private final Function<ConfigProperties, Stream<CfService>> servicesProvider;
    private final CloudLoggingCredentials.Parser credentialParser;

    public CloudLoggingSpanExporterProvider() {
        this(config -> new CloudLoggingServicesProvider(config).get(), CloudLoggingCredentials.parser());
    }

    CloudLoggingSpanExporterProvider(Function<ConfigProperties, Stream<CfService>> serviceProvider, CloudLoggingCredentials.Parser credentialParser) {
        this.servicesProvider = serviceProvider;
        this.credentialParser = credentialParser;
    }

    private static String getCompression(ConfigProperties config) {
        String compression = config.getString("otel.exporter.cloud-logging.traces.compression");
        return compression != null ? compression : config.getString("otel.exporter.cloud-logging.compression", "gzip");
    }

    private static Duration getTimeOut(ConfigProperties config) {
        Duration timeout = config.getDuration("otel.exporter.cloud-logging.traces.timeout");
        return timeout != null ? timeout : config.getDuration("otel.exporter.cloud-logging.timeout");
    }

    @Override
    public String getName() {
        return "cloud-logging";
    }

    @Override
    public SpanExporter createExporter(ConfigProperties config) {
        List<SpanExporter> exporters = servicesProvider.apply(config)
                .map(svc -> createExporter(config, svc))
                .filter(exp -> !(exp instanceof NoopSpanExporter))
                .collect(Collectors.toList());
        return SpanExporter.composite(exporters);
    }

    private SpanExporter createExporter(ConfigProperties config, CfService service) {
        LOG.info("Creating span exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        CfCredentials cfCredentials = service.getCredentials();
        CloudLoggingCredentials credentials = credentialParser.parse(cfCredentials);
        if (!credentials.validate()) {
            return NoopSpanExporter.getInstance();
        }

        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder();
        builder.setEndpoint(credentials.getEndpoint())
                .setCompression(getCompression(config))
                .setClientTls(credentials.getClientKey(), credentials.getClientCert())
                .setTrustedCertificates(credentials.getServerCert())
                .setRetryPolicy(RetryPolicy.getDefault());

        Duration timeOut = getTimeOut(config);
        if (timeOut != null) {
            builder.setTimeout(timeOut);
        }

        LOG.info("Created span exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        return builder.build();
    }
}
