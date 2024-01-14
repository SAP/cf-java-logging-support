package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudLoggingBindingPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final Logger LOG = Logger.getLogger(CloudLoggingBindingPropertiesSupplier.class.getName());
    private static final String OTLP_ENDPOINT = "ingest-otlp-endpoint";
    private static final String OTLP_CLIENT_KEY = "ingest-otlp-key";
    private static final String OTLP_CLIENT_CERT = "ingest-otlp-cert";
    private static final String OTLP_SERVER_CERT = "server-ca";

    private final CloudLoggingServicesProvider cloudLoggingServicesProvider;

    public CloudLoggingBindingPropertiesSupplier() {
        this(new CloudLoggingServicesProvider(getDefaultProperties(), new CloudFoundryServicesAdapter(new CfEnv())));
    }

    CloudLoggingBindingPropertiesSupplier(CloudLoggingServicesProvider cloudLoggingServicesProvider) {
        this.cloudLoggingServicesProvider = cloudLoggingServicesProvider;
    }

    private static ConfigProperties getDefaultProperties() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("com.sap.otel.extension.cloud-logging.label", "cloud-logging");
        defaults.put("com.sap.otel.extension.cloud-logging.tag", "Cloud Logging");
        defaults.put("otel.javaagent.extension.sap.cf.binding.user-provided.label", "user-provided");
        return DefaultConfigProperties.create(defaults);
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static File writeFile(String prefix, String suffix, String content) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        try (FileWriter writer = new FileWriter(file)) {
            writer.append(content);
            LOG.fine("Created temporary file " + file.getAbsolutePath());
        }
        return file;
    }

    /**
     * Scans service bindings, both managed and user-provided for Cloud Logging.
     * Managed services require the label "cloud-logging" to be considered.
     * Services will be selected by the tag "Cloud Logging".
     * User-provided services will be preferred over managed service instances.
     *
     * @return The pre-configured connection properties for the OpenTelemetry SDK.
     */
    @Override
    public Map<String, String> get() {
        return cloudLoggingServicesProvider.get()
                .findFirst()
                .map(this::createEndpointConfiguration).orElseGet(Collections::emptyMap);
    }

    private Map<String, String> createEndpointConfiguration(CfService svc) {
        LOG.config("Using service " + svc.getName() + " (" + svc.getLabel() + ")");

        String endpoint = svc.getCredentials().getString(OTLP_ENDPOINT);
        if (isBlank(endpoint)) {
            LOG.warning("Credential \"" + OTLP_ENDPOINT + "\" not found. Skipping OTLP exporter configuration");
            return Collections.emptyMap();
        }
        String clientKey = svc.getCredentials().getString(OTLP_CLIENT_KEY);
        if (isBlank(clientKey)) {
            LOG.warning("Credential \"" + OTLP_CLIENT_KEY + "\" not found. Skipping OTLP exporter configuration");
            return Collections.emptyMap();
        }
        String clientCert = svc.getCredentials().getString(OTLP_CLIENT_CERT);
        if (isBlank(clientCert)) {
            LOG.warning("Credential \"" + OTLP_CLIENT_CERT + "\" not found. Skipping OTLP exporter configuration");
            return Collections.emptyMap();
        }
        String serverCert = svc.getCredentials().getString(OTLP_SERVER_CERT);
        if (isBlank(serverCert)) {
            LOG.warning("Credential \"" + OTLP_SERVER_CERT + "\" not found. Skipping OTLP exporter configuration");
            return Collections.emptyMap();
        }

        try {
            File clientKeyFile = writeFile("cloud-logging-client", ".key", clientKey);
            File clientCertFile = writeFile("cloud-logging-client", ".cert", clientCert);
            File serverCertFile = writeFile("cloud-logging-server", ".cert", serverCert);

            HashMap<String, String> properties = new HashMap<>();
            properties.put("otel.exporter.otlp.endpoint", "https://" + endpoint);
            properties.put("otel.exporter.otlp.client.key", clientKeyFile.getAbsolutePath());
            properties.put("otel.exporter.otlp.client.certificate", clientCertFile.getAbsolutePath());
            properties.put("otel.exporter.otlp.certificate", serverCertFile.getAbsolutePath());

            properties.put("otel.exporter.protocol", "grpc");
            properties.put("otel.exporter.compression", "gzip");

            return properties;
        } catch (IOException cause) {
            LOG.log(Level.WARNING, "Cannot create TLS certificate or key files", cause);
            return Collections.emptyMap();
        }
    }

}
