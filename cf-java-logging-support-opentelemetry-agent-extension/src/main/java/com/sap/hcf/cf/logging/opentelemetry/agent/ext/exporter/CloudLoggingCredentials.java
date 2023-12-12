package com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter;

import io.pivotal.cfenv.core.CfCredentials;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

class CloudLoggingCredentials {

    private static final Logger LOG = Logger.getLogger(CloudLoggingCredentials.class.getName());

    private static final Parser PARSER = new Parser();

    private static final String CRED_OTLP_ENDPOINT = "ingest-otlp-endpoint";
    private static final String CRED_OTLP_CLIENT_KEY = "ingest-otlp-key";
    private static final String CRED_OTLP_CLIENT_CERT = "ingest-otlp-cert";
    private static final String CRED_OTLP_SERVER_CERT = "server-ca";
    private static final String CLOUD_LOGGING_ENDPOINT_PREFIX = "https://";


    private String endpoint;
    private byte[] clientKey;
    private byte[] clientCert;
    private byte[] serverCert;

    private CloudLoggingCredentials() {
    }

    static CloudLoggingCredentials.Parser parser() {
        return PARSER;
    }

    private static byte[] getPEMBytes(CfCredentials credentials, String key) {
        String raw = credentials.getString(key);
        return raw == null ? null : raw.trim().replace("\\n", "\n").getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static boolean isNullOrEmpty(byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    public boolean validate() {
        if (isBlank(endpoint)) {
            LOG.warning("Credential \"" + CRED_OTLP_ENDPOINT + "\" not found. Skipping cloud-logging exporter configuration");
            return false;
        }

        if (isNullOrEmpty(clientKey)) {
            LOG.warning("Credential \"" + CRED_OTLP_CLIENT_KEY + "\" not found. Skipping cloud-logging exporter configuration");
            return false;
        }

        if (isNullOrEmpty(clientCert)) {
            LOG.warning("Credential \"" + CRED_OTLP_CLIENT_CERT + "\" not found. Skipping cloud-logging exporter configuration");
            return false;
        }

        if (isNullOrEmpty(serverCert)) {
            LOG.warning("Credential \"" + CRED_OTLP_SERVER_CERT + "\" not found. Skipping cloud-logging exporter configuration");
            return false;
        }
        return true;
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

    static class Parser {
        CloudLoggingCredentials parse(CfCredentials cfCredentials) {
            CloudLoggingCredentials parsed = new CloudLoggingCredentials();
            String rawEndpoint = cfCredentials.getString(CRED_OTLP_ENDPOINT);
            parsed.endpoint = isBlank(rawEndpoint) ? null : CLOUD_LOGGING_ENDPOINT_PREFIX + rawEndpoint;
            parsed.clientKey = getPEMBytes(cfCredentials, CRED_OTLP_CLIENT_KEY);
            parsed.clientCert = getPEMBytes(cfCredentials, CRED_OTLP_CLIENT_CERT);
            parsed.serverCert = getPEMBytes(cfCredentials, CRED_OTLP_SERVER_CERT);
            return parsed;
        }
    }
}
