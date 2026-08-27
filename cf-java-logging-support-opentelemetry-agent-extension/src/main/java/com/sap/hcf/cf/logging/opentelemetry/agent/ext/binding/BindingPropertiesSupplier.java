package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic {@link Supplier} that turns a Cloud Foundry service binding into OpenTelemetry OTLP exporter properties.
 * <p>
 * It is freely configurable:
 * <ul>
 *   <li>binding discovery is delegated to the injected {@code serviceProvider};</li>
 *   <li>the credential field names and temp-file prefixes are provided via {@link BindingCredentialKeys};</li>
 *   <li>an optional {@code urlTransform} function rewrites the raw endpoint URL before scheme prepending;</li>
 *   <li>an optional {@code scheme} prefix is prepended to the (possibly transformed) URL.</li>
 * </ul>
 * A server CA certificate ({@link BindingCredentialKeys#serverCaCertKey}) is required when mTLS client credentials are
 * also present; if it is absent or blank in that case, the binding is skipped and an empty map is returned. mTLS
 * (client certificate and key) and a Bearer token are optional. TLS files are written atomically before being merged
 * into the result map.
 */
class BindingPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final Logger LOG = Logger.getLogger(BindingPropertiesSupplier.class.getName());

    private final String description;
    private final Optional<CloudFoundryServiceInstance> instance;
    private final PemFileCreator pemFileCreator;
    private final BindingCredentialKeys keys;
    private final Function<String, String> urlTransform;
    private final String scheme;
    private final Function<String, String> serverCaCertProvider;
    private final boolean serverCaCertRequiresMtls;
    private final String protocol;
    private final String compression;

    private BindingPropertiesSupplier(Builder builder) {
        this.description = builder.description;
        this.instance = builder.instance;
        this.pemFileCreator = builder.pemFileCreator;
        this.keys = builder.keys;
        this.urlTransform = builder.urlTransform;
        this.scheme = builder.scheme;
        this.serverCaCertProvider = builder.serverCaCertProvider;
        this.serverCaCertRequiresMtls = builder.serverCaCertRequiresMtls;
        this.protocol = builder.protocol;
        this.compression = builder.compression;
    }

    static Builder builder(String description,
                           Optional<CloudFoundryServiceInstance> instance,
                           PemFileCreator pemFileCreator,
                           BindingCredentialKeys keys) {
        return new Builder(description, instance, pemFileCreator, keys);
    }

    static final class Builder {
        private final String description;
        private final Optional<CloudFoundryServiceInstance> instance;
        private final PemFileCreator pemFileCreator;
        private final BindingCredentialKeys keys;
        private Function<String, String> urlTransform;
        private String scheme;
        private Function<String, String> serverCaCertProvider;
        private boolean serverCaCertRequiresMtls = false;
        private String protocol = "http/protobuf";
        private String compression = "gzip";

        private Builder(String description,
                        Optional<CloudFoundryServiceInstance> instance,
                        PemFileCreator pemFileCreator,
                        BindingCredentialKeys keys) {
            this.description = description;
            this.instance = instance;
            this.pemFileCreator = pemFileCreator;
            this.keys = keys;
        }

        Builder urlTransform(Function<String, String> transform) { this.urlTransform = transform; return this; }
        Builder scheme(String scheme) { this.scheme = scheme; return this; }
        Builder serverCaCertProvider(Function<String, String> provider) { this.serverCaCertProvider = provider; return this; }
        /** When {@code true}, the server CA provider is only invoked when mTLS client credentials are present. */
        Builder serverCaCertRequiresMtls(boolean require) { this.serverCaCertRequiresMtls = require; return this; }
        Builder protocol(String protocol) { this.protocol = protocol; return this; }
        Builder compression(String compression) { this.compression = compression; return this; }

        BindingPropertiesSupplier build() {
            return new BindingPropertiesSupplier(this);
        }
    }

    @Override
    public Map<String, String> get() {
        if (!instance.isPresent()) {
            LOG.warning("Service binding '" + description + "' not found in VCAP_SERVICES");
            return Collections.emptyMap();
        }

        CloudFoundryCredentials creds = instance.get().getCredentials();
        if (creds == null) {
            LOG.warning("Service binding '" + description + "' has no credentials");
            return Collections.emptyMap();
        }

        String url = creds.getString(keys.endpointUrlKey);
        if (url == null || url.isBlank()) {
            LOG.warning("Service binding '" + description + "' has no endpoint URL in credential key '" + keys.endpointUrlKey + "'");
            return Collections.emptyMap();
        }

        if (urlTransform != null) {
            url = urlTransform.apply(url);
        }
        if (scheme != null) {
            url = scheme + url;
        }

        Optional<ClientCreds> clientCredsOpt = resolveClientCreds(creds);
        if (clientCredsOpt.isEmpty()) {
            return Collections.emptyMap();
        }
        ClientCreds clientCreds = clientCredsOpt.get();
        Optional<String> caCrt = (serverCaCertRequiresMtls && !clientCreds.hasMtls())
                ? Optional.empty()
                : resolveCaCert(creds, url);
        if (caCrt.isEmpty() && clientCreds.hasMtls()) {
            LOG.warning("Service binding '" + description + "' is missing server CA certificate.");
            return Collections.emptyMap();
        }

        Map<String, String> props = new LinkedHashMap<>();
        props.put("otel.exporter.otlp.endpoint", url);
        props.put("otel.exporter.otlp.protocol", protocol);
        props.put("otel.exporter.otlp.compression", compression);

        if (caCrt.isPresent()) {
            try {
                Map<String, String> tlsProps = new LinkedHashMap<>();
                File caFile = pemFileCreator.writeFile(keys.serverCaFilePrefix, ".crt", caCrt.get());
                tlsProps.put("otel.exporter.otlp.certificate", caFile.getAbsolutePath());
                if (clientCreds.hasMtls()) {
                    File certFile = pemFileCreator.writeFile(keys.clientCertFilePrefix, ".crt", clientCreds.cert.get());
                    File keyFile  = pemFileCreator.writeFile(keys.clientKeyFilePrefix,  ".key", clientCreds.key.get());
                    tlsProps.put("otel.exporter.otlp.client.certificate", certFile.getAbsolutePath());
                    tlsProps.put("otel.exporter.otlp.client.key",         keyFile.getAbsolutePath());
                }
                props.putAll(tlsProps);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to write TLS files for service binding '" + description + "'", e);
            }
        } else {
            LOG.warning("Service binding '" + description + "' is missing server CA certificate.");
        }

        Optional.ofNullable(keys.tokenKey)
                .map(creds::getString)
                .filter(t -> !t.isBlank())
                .ifPresent(t -> props.put("otel.exporter.otlp.headers", "Authorization=Bearer " + t));

        return props;
    }

    private Optional<ClientCreds> resolveClientCreds(CloudFoundryCredentials creds) {
        Optional<String> cert = Optional.ofNullable(creds.getString(keys.clientCertKey)).filter(s -> !s.isBlank());
        Optional<String> key  = Optional.ofNullable(creds.getString(keys.clientKeyKey)).filter(s -> !s.isBlank());
        if (cert.isPresent() != key.isPresent()) {
            LOG.warning("Service binding '" + description + "' has client " +
                    (cert.isPresent() ? "certificate but no client key." : "key but no client certificate."));
            return Optional.empty();
        }
        return Optional.of(new ClientCreds(cert, key));
    }

    private Optional<String> resolveCaCert(CloudFoundryCredentials creds, String url) {
        String result = serverCaCertProvider != null ? serverCaCertProvider.apply(url) : creds.getString(keys.serverCaCertKey);
        return Optional.ofNullable(result).filter(s -> !s.isBlank());
    }

    private static final class ClientCreds {
        final Optional<String> cert;
        final Optional<String> key;
        private ClientCreds(Optional<String> cert, Optional<String> key) { this.cert = cert; this.key = key; }
        boolean hasMtls() { return cert.isPresent(); }
    }
}
