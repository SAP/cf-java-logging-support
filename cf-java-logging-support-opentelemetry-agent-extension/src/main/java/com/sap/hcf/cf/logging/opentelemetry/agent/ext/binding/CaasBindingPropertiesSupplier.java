package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

/**
 * Configures the OpenTelemetry OTLP exporter from a Cloud Foundry CaaS (Container as a Service) service binding.
 * <p>
 * Reads mTLS credentials ({@code tls.crt}, {@code tls.key}) and the endpoint URL ({@code http-url}) from the CaaS
 * binding. The server CA certificate is downloaded from the endpoint URL rather than read from credentials. Port
 * {@code 4318} is substituted for the {@code <http-receiver-port>} placeholder that may appear in the binding URL.
 * The server CA download is skipped when no mTLS client credentials are present.
 */
public class CaasBindingPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final BindingCredentialKeys CAAS_KEYS = BindingCredentialKeys.builder()
            .endpointUrlKey("http-url")
            .clientCertKey("tls.crt")
            .clientKeyKey("tls.key")
            .clientCertFilePrefix("caas-client-cert-")
            .clientKeyFilePrefix("caas-client-key-")
            .serverCaFilePrefix("caas-server-cert-")
            .build();

    private final Supplier<Map<String, String>> delegate;

    public CaasBindingPropertiesSupplier() {
        this(new CaasServiceProvider(getDefaultConfigProperties()), new PemFileCreator(),
                new ServerCertificateDownloader());
    }

    CaasBindingPropertiesSupplier(CaasServiceProvider serviceProvider,
                                   PemFileCreator pemFileCreator,
                                   ServerCertificateDownloader serverCertificateDownloader) {
        this.delegate = BindingPropertiesSupplier.builder(
                        "caas",
                        Optional.ofNullable(serviceProvider.get()),
                        pemFileCreator,
                        CAAS_KEYS)
                .urlTransform(u -> u.replace("<http-receiver-port>", "4318"))
                .serverCaCertProvider(serverCertificateDownloader::download)
                .serverCaCertRequiresMtls(true)
                .build();
    }

    private static DefaultConfigProperties getDefaultConfigProperties() {
        ComponentLoader componentLoader =
                ComponentLoader.forClassLoader(DefaultConfigProperties.class.getClassLoader());
        return DefaultConfigProperties.create(emptyMap(), componentLoader);
    }

    @Override
    public Map<String, String> get() {
        return delegate.get();
    }
}
