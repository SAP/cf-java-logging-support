package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryServiceInstance;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.VcapServiceProvider;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.EXPORTER;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.lang.String.format;

public class VcapServiceLogsExporterProvider implements ConfigurableLogRecordExporterProvider {

    private static final Logger LOG = Logger.getLogger(VcapServiceLogsExporterProvider.class.getName());
    private static final String MSK_SKIP = " Skipping logs exporter registration.";
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

    public VcapServiceLogsExporterProvider() {
        this(config -> new VcapServiceProvider(config).get(),
             config -> VcapServiceCredentials.parser(config, RUNTIME.CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_LOGS_ENDPOINT),
             new ServerCertificateDownloader());
    }

    VcapServiceLogsExporterProvider(Function<ConfigProperties, CloudFoundryServiceInstance> serviceProvider,
                                    Function<ConfigProperties, Function<CloudFoundryCredentials, VcapServiceCredentials>> credentialParserProvider,
                                    ServerCertificateDownloader serverCertificateDownloader) {
        this.serviceProvider = serviceProvider;
        this.credentialParserProvider = credentialParserProvider;
        this.serverCertificateDownloader = serverCertificateDownloader;
    }

    private static void addTimeout(Consumer<Duration> setter, ConfigProperties config) {
        Duration timeout = EXPORTER.VCAP_SERVICE.LOGS.TIMEOUT.getValue(config);
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

    @Override
    public String getName() {
        return "vcap-service";
    }

    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
        CloudFoundryServiceInstance serviceInstance = serviceProvider.apply(config);
        if (serviceInstance == null) {
            LOG.info(MSG_NO_BINDING);
            return NoopLogRecordExporter.getInstance();
        }
        CloudFoundryCredentials rawCredentials = serviceInstance.getCredentials();
        if (rawCredentials == null) {
            LOG.warning(format(MSG_NO_CREDENTIALS, serviceInstance.getName(), serviceInstance.getLabel()));
            return NoopLogRecordExporter.getInstance();
        }
        VcapServiceCredentials credentials = credentialParserProvider.apply(config).apply(rawCredentials);
        if (credentials == null || !credentials.validate()) {
            LOG.warning(format(MSG_INVALID_CREDENTIALS, serviceInstance.getName(), serviceInstance.getLabel()));
            return NoopLogRecordExporter.getInstance();
        }

        switch (EXPORTER.VCAP_SERVICE.LOGS.PROTOCOL.getValue(config)) {
        case "http/protobuf":
            return createHttpProtobufLogRecordExporter(config, credentials, serviceInstance);
        case "grpc":
            return createGrpcLogRecordExporter(config, credentials, serviceInstance);
        default:
            LOG.warning(format(MSG_UNSUPPORTED_PROTOCOL, EXPORTER.VCAP_SERVICE.LOGS.PROTOCOL.getValue(config),
                               serviceInstance.getName(), serviceInstance.getLabel()));

        }
        return NoopLogRecordExporter.getInstance();
    }

    private LogRecordExporter createGrpcLogRecordExporter(ConfigProperties config, VcapServiceCredentials credentials,
                                                          CloudFoundryServiceInstance serviceInstance) {
        OtlpGrpcLogRecordExporterBuilder builder =
                OtlpGrpcLogRecordExporter.builder().setEndpoint(credentials.getEndpoint())
                                         .setCompression(EXPORTER.VCAP_SERVICE.LOGS.COMPRESSION.getValue(config))
                                         .setRetryPolicy(RetryPolicy.getDefault());
        addTimeout(builder::setTimeout, config);
        if (!addTlsConfig(builder::setClientTls, builder::setTrustedCertificates, credentials,
                          s -> format(s, serviceInstance.getName(), serviceInstance.getLabel()))) {
            return NoopLogRecordExporter.getInstance();
        }
        addAuthTokenHeader(builder::addHeader, config, credentials);
        return builder.build();
    }

    private LogRecordExporter createHttpProtobufLogRecordExporter(ConfigProperties config,
                                                                  VcapServiceCredentials credentials,
                                                                  CloudFoundryServiceInstance serviceInstance) {
        OtlpHttpLogRecordExporterBuilder builder =
                OtlpHttpLogRecordExporter.builder().setEndpoint(credentials.getEndpoint())
                                         .setCompression(EXPORTER.VCAP_SERVICE.LOGS.COMPRESSION.getValue(config))
                                         .setRetryPolicy(RetryPolicy.getDefault());
        addTimeout(builder::setTimeout, config);
        if (!addTlsConfig(builder::setClientTls, builder::setTrustedCertificates, credentials,
                          s -> format(s, serviceInstance.getName(), serviceInstance.getLabel()))) {
            return NoopLogRecordExporter.getInstance();
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
