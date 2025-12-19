package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.PemFileCreator;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.tls.ServerCertificateDownloader;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;

public class CaasBindingPropertiesSupplier implements Supplier<Map<String, String>> {

    private static final String CAAS_CLIENT_KEY = "tls.key";
    private static final String CAAS_CLIENT_CERT = "tls.crt";
    private static final String CAAS_ENDPOINT = "http-url";
    private static final String CAAS_PORT_PLACEHOLDER = "<http-receiver-port>";
    private static final String PORT_OTLP_HTTP = "4318";

    private static final Logger LOG = Logger.getLogger(CaasBindingPropertiesSupplier.class.getName());

    private final CaasServiceProvider serviceProvider;
    private final PemFileCreator pemFileCreator;
    private final ServerCertificateDownloader serverCertificateDownloader;

    public CaasBindingPropertiesSupplier() {
        this(new CaasServiceProvider(getDefaultConfigProperties()), new PemFileCreator(),
             new ServerCertificateDownloader());
    }

    CaasBindingPropertiesSupplier(CaasServiceProvider serviceProvider, PemFileCreator pemFileCreator,
                                  ServerCertificateDownloader serverCertificateDownloader) {
        this.serviceProvider = serviceProvider;
        this.pemFileCreator = pemFileCreator;
        this.serverCertificateDownloader = serverCertificateDownloader;
    }

    private static DefaultConfigProperties getDefaultConfigProperties() {
        ComponentLoader componentLoader =
                ComponentLoader.forClassLoader(DefaultConfigProperties.class.getClassLoader());
        return DefaultConfigProperties.create(emptyMap(), componentLoader);
    }

    private static void putCaasDefaultProperties(Map<String, String> properties) {
        properties.put("otel.exporter.otlp.protocol", "http/protobuf");
        properties.put("otel.exporter.otlp.compression", "gzip");
    }

    @Override
    public Map<String, String> get() {
        CloudFoundryServiceInstance serviceInstance = serviceProvider.get();
        if (serviceInstance == null) {
            LOG.config("No CaaS service instance found.");
            return emptyMap();
        }
        CloudFoundryCredentials credentials = serviceInstance.getCredentials();
        if (credentials == null) {
            LOG.warning(() -> "CaaS service instance '" + serviceInstance.getName() + "' has no credentials.");
            return emptyMap();
        }
        String endpointUrl = credentials.getString(CAAS_ENDPOINT);
        if (endpointUrl == null || endpointUrl.isBlank()) {
            LOG.warning(() -> "CaaS service instance '" + serviceInstance.getName() + "' has no endpoint URL.");
            return emptyMap();
        }

        Map<String, String> properties = new HashMap<>();
        endpointUrl = endpointUrl.replace(CAAS_PORT_PLACEHOLDER, PORT_OTLP_HTTP);
        LOG.config("Using CaaS OTLP endpoint URL: " + endpointUrl);
        properties.put("otel.exporter.otlp.endpoint", endpointUrl);

        putCaasDefaultProperties(properties);

        String clientCert = credentials.getString(CAAS_CLIENT_CERT);
        String clientKey = credentials.getString(CAAS_CLIENT_KEY);
        if (clientCert != null && clientKey != null) {
            try {
                String serverCert = serverCertificateDownloader.download(endpointUrl);
                if (serverCert == null || serverCert.isBlank()) {
                    return properties;
                }
                File serverCertFile = pemFileCreator.writeFile("caas-server-cert-", ".crt", serverCert);
                File clientCertFile = pemFileCreator.writeFile("caas-client-cert-", ".crt", clientCert);
                File clientKeyFile = pemFileCreator.writeFile("caas-client-key-", ".key", clientKey);

                properties.put("otel.exporter.otlp.certificate", serverCertFile.getAbsolutePath());
                properties.put("otel.exporter.otlp.client.certificate", clientCertFile.getAbsolutePath());
                properties.put("otel.exporter.otlp.client.key", clientKeyFile.getAbsolutePath());

            } catch (IOException e) {
                LOG.warning(
                        () -> "Failed to create PEM files for CaaS service instance '" + serviceInstance.getName() + "': " + e.getMessage());
            }
        } else {
            LOG.warning(
                    () -> "CaaS service instance '" + serviceInstance.getName() + "' is missing client certificate or key.");
        }
        return properties;
    }

}
