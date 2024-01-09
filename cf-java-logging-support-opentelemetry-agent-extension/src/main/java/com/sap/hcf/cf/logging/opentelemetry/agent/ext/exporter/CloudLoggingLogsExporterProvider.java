package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingServicesProvider;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudLoggingLogsExporterProvider implements ConfigurableLogRecordExporterProvider {

    private static final Logger LOG = Logger.getLogger(CloudLoggingLogsExporterProvider.class.getName());

    private final Function<ConfigProperties, Stream<CfService>> servicesProvider;
    private final CloudLoggingCredentials.Parser credentialParser;

    public CloudLoggingLogsExporterProvider() {
        this(config -> new CloudLoggingServicesProvider(config, new CfEnv()).get(), CloudLoggingCredentials.parser());
    }

    CloudLoggingLogsExporterProvider(Function<ConfigProperties, Stream<CfService>> serviceProvider, CloudLoggingCredentials.Parser credentialParser) {
        this.servicesProvider = serviceProvider;
        this.credentialParser = credentialParser;
    }

    private static String getCompression(ConfigProperties config) {
        String compression = config.getString("otel.exporter.cloud-logging.logs.compression");
        return compression != null ? compression : config.getString("otel.exporter.cloud-logging.compression", "gzip");
    }

    private static Duration getTimeOut(ConfigProperties config) {
        Duration timeout = config.getDuration("otel.exporter.cloud-logging.logs.timeout");
        return timeout != null ? timeout : config.getDuration("otel.exporter.cloud-logging.timeout");
    }

    @Override
    public String getName() {
        return "cloud-logging";
    }

    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
        List<LogRecordExporter> exporters = servicesProvider.apply(config)
                .map(svc -> createExporter(config, svc))
                .filter(exp -> !(exp instanceof NoopLogRecordExporter))
                .collect(Collectors.toList());
        return LogRecordExporter.composite(exporters);
    }

    private LogRecordExporter createExporter(ConfigProperties config, CfService service) {
        LOG.info("Creating logs exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        CloudLoggingCredentials credentials = credentialParser.parse(service.getCredentials());
        if (!credentials.validate()) {
            return NoopLogRecordExporter.getInstance();
        }

        OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder();
        builder.setEndpoint(credentials.getEndpoint())
                .setCompression(getCompression(config))
                .setClientTls(credentials.getClientKey(), credentials.getClientCert())
                .setTrustedCertificates(credentials.getServerCert())
                .setRetryPolicy(RetryPolicy.getDefault());

        Duration timeOut = getTimeOut(config);
        if (timeOut != null) {
            builder.setTimeout(timeOut);
        }

        LOG.info("Created logs exporter for service binding " + service.getName() + " (" + service.getLabel() + ")");
        return builder.build();
    }
}
