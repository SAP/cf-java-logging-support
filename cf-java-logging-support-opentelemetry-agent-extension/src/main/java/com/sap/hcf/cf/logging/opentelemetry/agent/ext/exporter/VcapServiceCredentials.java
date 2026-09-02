package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudFoundryCredentials;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ConfigProperty;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.function.Function;
import java.util.logging.Logger;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ExtensionConfigurations.RUNTIME.CLOUD_FOUNDRY;

public class VcapServiceCredentials {

    private static final Logger LOG = Logger.getLogger(VcapServiceCredentials.class.getName());
    private final String endpoint;
    private final byte[] clientKey;
    private final byte[] clientCert;
    private final byte[] serverCert;
    private final String authToken;

    public VcapServiceCredentials(String endpoint, byte[] clientKey, byte[] clientCert, byte[] serverCert,
                                  String authToken) {
        this.endpoint = endpoint;
        this.clientKey = clientKey;
        this.clientCert = clientCert;
        this.serverCert = serverCert;
        this.authToken = authToken;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public byte[] getClientKey() {
        return clientKey;
    }

    public byte[] getClientCert() {
        return clientCert;
    }

    public byte[] getServerCert() {
        return serverCert;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean validate() {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            LOG.warning(
                    "Generic VCAP service credential property for the endpoint is not configured. Skipping credential parsing.");
            return false;
        }
        if (authToken != null && !authToken.trim().isEmpty()) {
            return true;
        }
        if (clientKey == null || clientKey.length == 0 || clientCert == null || clientCert.length == 0) {
            LOG.warning(
                    "Generic VCAP service credential properties for the client key or certificate missing or incomplete. Skipping credential parsing.");
            return false;
        }
        return true;
    }

    static Function<CloudFoundryCredentials, VcapServiceCredentials> parser(ConfigProperties config) {
        return parser(config, CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_ENDPOINT);
    }

    static Function<CloudFoundryCredentials, VcapServiceCredentials> parser(ConfigProperties config,
                                                                             ConfigProperty<String> endpointProperty) {
        String endpointName = endpointProperty.getValue(config);
        String clientKeyName = CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_CLIENT_KEY.getValue(config);
        String clientCertName = CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_CLIENT_CERT.getValue(config);
        String serverCertName = CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_SERVER_CERT.getValue(config);
        String authTokenName = CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.AUTH_TOKEN.getValue(config);
        if (endpointName == null) {
            LOG.info(
                    "Generic VCAP service credential property \"" + CLOUD_FOUNDRY.SERVICE.VCAP_SERVICE.CREDENTIALS.OTLP_ENDPOINT.getKey() + "\" is not configured. Skipping credential parsing.");
            return ignored -> null;
        }
        return new Parser(endpointName, clientKeyName, clientCertName, serverCertName, authTokenName);
    }

    static class Parser implements Function<CloudFoundryCredentials, VcapServiceCredentials> {
        private final String endpointName;
        private final String clientKeyName;
        private final String clientCertName;
        private final String serverCertName;
        private final String authTokenName;

        private Parser(String endpointName, String clientKeyName, String clientCertName, String serverCertName,
                       String authTokenName) {
            this.endpointName = endpointName;
            this.clientKeyName = clientKeyName;
            this.clientCertName = clientCertName;
            this.serverCertName = serverCertName;
            this.authTokenName = authTokenName;
        }

        @Override
        public VcapServiceCredentials apply(CloudFoundryCredentials cloudFoundryCredentials) {
            return new VcapServiceCredentials(cloudFoundryCredentials.getString(endpointName),
                                              cloudFoundryCredentials.getPEMBytes(clientKeyName),
                                              cloudFoundryCredentials.getPEMBytes(clientCertName),
                                              cloudFoundryCredentials.getPEMBytes(serverCertName),
                                              cloudFoundryCredentials.getString(authTokenName));
        }
    }
}
