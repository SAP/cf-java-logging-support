package com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding;

/**
 * Immutable configuration describing which credential fields of a Cloud Foundry service binding hold the connection
 * endpoint, the (optional) mTLS client certificate and key, the required server CA certificate and the optional bearer
 * token, together with the temp-file name prefixes used when materializing the PEM files.
 */
class BindingCredentialKeys {

    final String endpointUrlKey;
    final String clientCertKey;
    final String clientKeyKey;
    final String serverCaCertKey;
    final String tokenKey;
    final String clientCertFilePrefix;
    final String clientKeyFilePrefix;
    final String serverCaFilePrefix;

    private BindingCredentialKeys(Builder builder) {
        this.endpointUrlKey = builder.endpointUrlKey;
        this.clientCertKey = builder.clientCertKey;
        this.clientKeyKey = builder.clientKeyKey;
        this.serverCaCertKey = builder.serverCaCertKey;
        this.tokenKey = builder.tokenKey;
        this.clientCertFilePrefix = builder.clientCertFilePrefix;
        this.clientKeyFilePrefix = builder.clientKeyFilePrefix;
        this.serverCaFilePrefix = builder.serverCaFilePrefix;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private String endpointUrlKey;
        private String clientCertKey;
        private String clientKeyKey;
        private String serverCaCertKey;
        private String tokenKey;
        private String clientCertFilePrefix = "client-cert-";
        private String clientKeyFilePrefix = "client-key-";
        private String serverCaFilePrefix = "server-ca-";

        Builder endpointUrlKey(String key) {
            this.endpointUrlKey = key;
            return this;
        }

        Builder clientCertKey(String key) {
            this.clientCertKey = key;
            return this;
        }

        Builder clientKeyKey(String key) {
            this.clientKeyKey = key;
            return this;
        }

        Builder serverCaCertKey(String key) {
            this.serverCaCertKey = key;
            return this;
        }

        Builder tokenKey(String key) {
            this.tokenKey = key;
            return this;
        }

        Builder clientCertFilePrefix(String prefix) {
            this.clientCertFilePrefix = prefix;
            return this;
        }

        Builder clientKeyFilePrefix(String prefix) {
            this.clientKeyFilePrefix = prefix;
            return this;
        }

        Builder serverCaFilePrefix(String prefix) {
            this.serverCaFilePrefix = prefix;
            return this;
        }

        BindingCredentialKeys build() {
            return new BindingCredentialKeys(this);
        }
    }
}
