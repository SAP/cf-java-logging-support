package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Configures the OpenTelemetry OTLP exporter from a generic Cloud Foundry service binding. The binding is selected by
 * instance name ({@code sap.otel.generic.cf.binding.name}), service label ({@code sap.otel.generic.cf.binding.label})
 * or tag ({@code sap.otel.generic.cf.binding.tag}), and every credential field name plus the OTLP protocol and
 * compression are fully configurable.
 * <p>
 * This is a thin, opt-in wrapper around the generic {@link BindingPropertiesSupplier}. When none of the three selector
 * properties are configured, the wrapper is disabled and returns an empty map without ever invoking the service
 * provider. Credential field names default to the OTel Collector conventions ({@code url} for the endpoint,
 * {@code tls.crt}/{@code tls.key} for optional mTLS, {@code tls.ca.crt} for the required server CA certificate and
 * {@code token} for the optional Bearer authorization header) and can be overridden individually.
 */
public class ConfigurableOtelBindingPropertiesSupplier implements Supplier<Map<String, String>> {

    // Credential field names, OTLP protocol and compression. All are configurable and fall back to the defaults below.
    // NOTE: these are read directly from the ConfigProperties parameter rather than modeled as
    // ConfigProperty<String> constants, because ConfigProperty#stringValued is package-private to the config package.
    private static final String ENDPOINT_NAME_PROP = "sap.otel.generic.cf.binding.endpoint-name";
    private static final String CLIENT_CERT_NAME_PROP = "sap.otel.generic.cf.binding.client-cert-name";
    private static final String CLIENT_KEY_NAME_PROP = "sap.otel.generic.cf.binding.client-key-name";
    private static final String SERVER_CA_NAME_PROP = "sap.otel.generic.cf.binding.server-ca-name";
    private static final String TOKEN_NAME_PROP = "sap.otel.generic.cf.binding.token-name";
    private static final String PROTOCOL_PROP = "sap.otel.generic.cf.binding.protocol";
    private static final String COMPRESSION_PROP = "sap.otel.generic.cf.binding.compression";

    private static final String ENDPOINT_NAME_DEFAULT = "url";
    private static final String CLIENT_CERT_NAME_DEFAULT = "tls.crt";
    private static final String CLIENT_KEY_NAME_DEFAULT = "tls.key";
    private static final String SERVER_CA_NAME_DEFAULT = "tls.ca.crt";
    private static final String TOKEN_NAME_DEFAULT = "token";
    private static final String PROTOCOL_DEFAULT = "http/protobuf";
    private static final String COMPRESSION_DEFAULT = "gzip";

    // Temp-file name prefixes are fixed internally (not configurable).
    private static final String CLIENT_CERT_FILE_PREFIX = "otel-generic-client-cert-";
    private static final String CLIENT_KEY_FILE_PREFIX = "otel-generic-client-key-";
    private static final String SERVER_CA_FILE_PREFIX = "otel-generic-server-ca-";

    private final Supplier<Map<String, String>> delegate;

    /**
     * Creates a new instance using default service discovery and TLS infrastructure.
     */
    public ConfigurableOtelBindingPropertiesSupplier() {
        this(CloudFoundryServicesAdapter.builder().build(), new PemFileCreator(), getDefaultConfigProperties());
    }

    ConfigurableOtelBindingPropertiesSupplier(CloudFoundryServicesAdapter adapter,
                                              PemFileCreator pemFileCreator,
                                              ConfigProperties config) {
        String selector = Stream.of(
                ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.GENERIC.BINDING_NAME.getValue(config),
                ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.GENERIC.BINDING_LABEL.getValue(config),
                ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY.SERVICE.GENERIC.BINDING_TAG.getValue(config)
        ).filter(s -> s != null && !s.isBlank()).findFirst().orElse(null);

        if (selector == null) {
            this.delegate = () -> emptyMap();
            return;
        }

        BindingCredentialKeys keys = BindingCredentialKeys.builder()
                .endpointUrlKey(get(config, ENDPOINT_NAME_PROP, ENDPOINT_NAME_DEFAULT))
                .clientCertKey(get(config, CLIENT_CERT_NAME_PROP, CLIENT_CERT_NAME_DEFAULT))
                .clientKeyKey(get(config, CLIENT_KEY_NAME_PROP, CLIENT_KEY_NAME_DEFAULT))
                .serverCaCertKey(get(config, SERVER_CA_NAME_PROP, SERVER_CA_NAME_DEFAULT))
                .tokenKey(get(config, TOKEN_NAME_PROP, TOKEN_NAME_DEFAULT))
                .clientCertFilePrefix(CLIENT_CERT_FILE_PREFIX)
                .clientKeyFilePrefix(CLIENT_KEY_FILE_PREFIX)
                .serverCaFilePrefix(SERVER_CA_FILE_PREFIX)
                .build();

        this.delegate = BindingPropertiesSupplier.builder(
                        selector,
                        new GenericBindingServiceProvider(adapter, selector).get(),
                        pemFileCreator,
                        keys)
                .protocol(get(config, PROTOCOL_PROP, PROTOCOL_DEFAULT))
                .compression(get(config, COMPRESSION_PROP, COMPRESSION_DEFAULT))
                .build();
    }

    private static String get(ConfigProperties config, String key, String defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        String value = config.getString(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static ConfigProperties getDefaultConfigProperties() {
        ComponentLoader componentLoader =
                ComponentLoader.forClassLoader(DefaultConfigProperties.class.getClassLoader());
        return DefaultConfigProperties.create(emptyMap(), componentLoader);
    }

    /**
     * Reads the generic service binding credentials from {@code VCAP_SERVICES} and returns the OpenTelemetry OTLP
     * exporter configuration properties. Returns an empty map when none of the selector properties
     * ({@code sap.otel.generic.cf.binding.name}, {@code …label}, {@code …tag}) are configured, when no matching binding
     * is found, or when mTLS client credentials are present but the server CA certificate is absent. Supports
     * server-CA-only TLS, mTLS, and optional Bearer token authentication.
     *
     * @return the pre-configured connection properties for the OpenTelemetry SDK, or an empty map when no usable
     * binding is found.
     */
    @Override
    public Map<String, String> get() {
        return delegate.get();
    }
}
