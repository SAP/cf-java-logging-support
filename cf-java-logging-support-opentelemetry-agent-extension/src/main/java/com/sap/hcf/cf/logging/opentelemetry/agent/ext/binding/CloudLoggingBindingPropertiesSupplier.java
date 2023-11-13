package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

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
import java.util.stream.Stream;

public class CloudLoggingBindingPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final Logger LOG = Logger.getLogger(CloudLoggingBindingPropertiesSupplier.class.getName());
    private static final String CLOUD_LOGGING_LABEL = System.getProperty("com.sap.otel.extension.cloud-logging.label", "cloud-logging");
    private static final String CLOUD_LOGGING_TAG = System.getProperty("com.sap.otel.extension.cloud-logging.tag", "Cloud Logging");
    private static final String USER_PROVIDED_LABEL = "user-provided";
    public static final String OTLP_ENDPOINT = "ingest-otlp-endpoint";
    public static final String OTLP_CLIENT_KEY = "ingest-otlp-key";
    public static final String OTLP_CLIENT_CERT = "ingest-otlp-cert";
    public static final String OTLP_SERVER_CERT = "server-ca";

    private final CfEnv cfEnv;

    public CloudLoggingBindingPropertiesSupplier(CfEnv cfEnv) {
        this.cfEnv = cfEnv;
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
        Stream<CfService> userProvided = cfEnv.findServicesByLabel(USER_PROVIDED_LABEL).stream();
        Stream<CfService> managed = cfEnv.findServicesByLabel(CLOUD_LOGGING_LABEL).stream();
        return Stream.concat(userProvided, managed)
                .filter(svc -> svc.existsByTagIgnoreCase(CLOUD_LOGGING_TAG))
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

}
