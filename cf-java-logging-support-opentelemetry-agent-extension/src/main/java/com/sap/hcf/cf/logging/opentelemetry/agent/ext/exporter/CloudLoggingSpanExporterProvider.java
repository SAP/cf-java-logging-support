package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingServicesProvider;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.EXPORTER;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudLoggingSpanExporterProvider implements ConfigurableSpanExporterProvider {

    private static final Logger LOG = Logger.getLogger(CloudLoggingSpanExporterProvider.class.getName());

    private final Function<ConfigProperties, Stream<CloudFoundryServiceInstance>> servicesProvider;
    private final CloudLoggingCredentials.Parser credentialParser;

    public CloudLoggingSpanExporterProvider() {
        this(config -> new CloudLoggingServicesProvider(config).get(), CloudLoggingCredentials.parser());
    }

    CloudLoggingSpanExporterProvider(Function<ConfigProperties, Stream<CloudFoundryServiceInstance>> serviceProvider,
                                     CloudLoggingCredentials.Parser credentialParser) {
        this.servicesProvider = serviceProvider;
        this.credentialParser = credentialParser;
    }

    private static String getCompression(ConfigProperties config) {
        return EXPORTER.CLOUD_LOGGING.TRACES.COMPRESSION.getValue(config);
    }

    private static Duration getTimeOut(ConfigProperties config) {
        return EXPORTER.CLOUD_LOGGING.TRACES.TIMEOUT.getValue(config);
    }

    @Override
    public String getName() {
        return "cloud-logging";
    }

    @Override
    public SpanExporter createExporter(ConfigProperties config) {
        List<SpanExporter> exporters = servicesProvider.apply(config).map(svc -> createExporter(config, svc))
                                                       .filter(exp -> !(exp instanceof NoopSpanExporter))
                                                       .collect(Collectors.toList());
        return SpanExporter.composite(exporters);
    }

    private SpanExporter createExporter(ConfigProperties config, CloudFoundryServiceInstance service) {
        LOG.info("Creating span exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        CloudLoggingCredentials credentials = credentialParser.parse(service.getCredentials());
        if (!credentials.validate()) {
            return NoopSpanExporter.getInstance();
        }

        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder();
        builder.setEndpoint(credentials.getEndpoint()).setCompression(getCompression(config))
               .setClientTls(credentials.getClientKey(), credentials.getClientCert())
               .setTrustedCertificates(credentials.getServerCert()).setRetryPolicy(RetryPolicy.getDefault());

        Duration timeOut = getTimeOut(config);
        if (timeOut != null) {
            builder.setTimeout(timeOut);
        }

        LOG.info("Created span exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        return builder.build();
    }
}
