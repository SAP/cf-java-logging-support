package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;

/**
 * Configures the OpenTelemetry OTLP exporter from a Cloud Foundry Cloud Logging service binding.
 * <p>
 * Selects the first Cloud Logging binding that carries both a client certificate ({@code ingest-otlp-cert}) and a
 * client key ({@code ingest-otlp-key}). Endpoint scheme, OTLP protocol, and compression are taken from
 * {@link com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations} and default to
 * {@code https://}, {@code grpc}, and {@code gzip} respectively.
 */
public class CloudLoggingBindingPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final Logger LOG = Logger.getLogger(CloudLoggingBindingPropertiesSupplier.class.getName());

    private static final String OTLP_ENDPOINT    = "ingest-otlp-endpoint";
    private static final String OTLP_CLIENT_KEY  = "ingest-otlp-key";
    private static final String OTLP_CLIENT_CERT = "ingest-otlp-cert";
    private static final String OTLP_SERVER_CERT = "server-ca";
    private static final String CLOUD_LOGGING_DESCRIPTION = "cloud-logging";

    private static final BindingCredentialKeys CLOUD_LOGGING_KEYS = BindingCredentialKeys.builder()
            .endpointUrlKey(OTLP_ENDPOINT)
            .clientCertKey(OTLP_CLIENT_CERT)
            .clientKeyKey(OTLP_CLIENT_KEY)
            .serverCaCertKey(OTLP_SERVER_CERT)
            .clientCertFilePrefix("cloud-logging-client-cert-")
            .clientKeyFilePrefix("cloud-logging-client-key-")
            .serverCaFilePrefix("cloud-logging-server-ca-")
            .build();

    private final Supplier<Map<String, String>> delegate;

    public CloudLoggingBindingPropertiesSupplier() {
        this(new CloudLoggingServicesProvider(
                DefaultConfigProperties.create(emptyMap(),
                        ComponentLoader.forClassLoader(DefaultConfigProperties.class.getClassLoader()))),
             new PemFileCreator());
    }

    CloudLoggingBindingPropertiesSupplier(CloudLoggingServicesProvider provider, PemFileCreator pemFileCreator) {
        this(provider, pemFileCreator,
                DefaultConfigProperties.create(emptyMap(),
                        ComponentLoader.forClassLoader(DefaultConfigProperties.class.getClassLoader())));
    }

    CloudLoggingBindingPropertiesSupplier(CloudLoggingServicesProvider provider,
                                          PemFileCreator pemFileCreator,
                                          ConfigProperties config) {
        String scheme      = ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.ENDPOINT_SCHEME.getValue(config);
        String protocol    = ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.PROTOCOL.getValue(config);
        String compression = ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.COMPRESSION.getValue(config);
        this.delegate = BindingPropertiesSupplier.builder(
                        CLOUD_LOGGING_DESCRIPTION,
                        findValidInstance(provider),
                        pemFileCreator,
                        CLOUD_LOGGING_KEYS)
                .scheme(scheme)
                .protocol(protocol)
                .compression(compression)
                .build();
    }

    private static Optional<CloudFoundryServiceInstance> findValidInstance(
            CloudLoggingServicesProvider provider) {
        return provider.get().findFirst().filter(instance -> {
            CloudFoundryCredentials creds = instance.getCredentials();
            if (creds == null) return false;
            String key = creds.getString(OTLP_CLIENT_KEY);
            if (key == null || key.isBlank()) {
                LOG.warning("Cloud Logging binding '" + CLOUD_LOGGING_DESCRIPTION
                        + "' has no '" + OTLP_CLIENT_KEY + "'");
                return false;
            }
            String cert = creds.getString(OTLP_CLIENT_CERT);
            if (cert == null || cert.isBlank()) {
                LOG.warning("Cloud Logging binding '" + CLOUD_LOGGING_DESCRIPTION
                        + "' has no '" + OTLP_CLIENT_CERT + "'");
                return false;
            }
            return true;
        });
    }

    /**
     * Reads the Cloud Logging service binding credentials from {@code VCAP_SERVICES} and returns the OpenTelemetry
     * OTLP exporter configuration properties. Returns an empty map when no Cloud Logging binding with both a client
     * certificate and client key is found, when the binding has no endpoint URL, or when the server CA certificate is
     * absent.
     *
     * @return the pre-configured connection properties for the OpenTelemetry SDK, or an empty map when no usable
     * binding is found.
     */
    @Override
    public Map<String, String> get() {
        return delegate.get();
    }
}
