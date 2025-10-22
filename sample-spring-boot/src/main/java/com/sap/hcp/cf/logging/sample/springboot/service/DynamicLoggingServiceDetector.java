package com.sap.hcp.cf.logging.sample.springboot.service;

import com.sap.cloud.environment.servicebinding.api.DefaultServiceBindingAccessor;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.environment.servicebinding.api.ServiceBindingAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Scans the CF service bindings for a service named {@value SERVICE_NAME} to load the public and private RSA keys for
 * the dynamic log level feature.
 */
public class DynamicLoggingServiceDetector {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicLoggingServiceDetector.class);
    private static final String SERVICE_NAME = "dynamic-log-level";

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public DynamicLoggingServiceDetector() {
        this(DefaultServiceBindingAccessor.getInstance());
    }

    DynamicLoggingServiceDetector(ServiceBindingAccessor accessor) {
        accessor.getServiceBindings().stream()
                .filter(b -> b.getServiceName().map(SERVICE_NAME::equalsIgnoreCase).orElse(false))
                .forEach(this::parseBinding);
    }

    private void parseBinding(ServiceBinding serviceBinding) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Credentials credentials = getCredentials(serviceBinding);
            RSAPrivateKey privateKeyCandidate = getPrivateKey(credentials, keyFactory);
            RSAPublicKey publicKeyCandidate = getPublicKey(credentials, keyFactory);
            if (privateKeyCandidate != null && publicKeyCandidate != null) {
                this.privateKey = privateKeyCandidate;
                this.publicKey = publicKeyCandidate;
            }
        } catch (Exception ex) {
            LOG.warn("Found service binding to {}({}). But could not read RSA keys.", serviceBinding.getName(),
                     serviceBinding.getServiceIdentifier(), ex);
        }
    }

    private static Credentials getCredentials(ServiceBinding serviceBinding) {
        return new Credentials(serviceBinding.getCredentials().get("privateKey").toString(),
                               serviceBinding.getCredentials().get("publicKey").toString());
    }

    private static RSAPrivateKey getPrivateKey(Credentials credentials, KeyFactory keyFactory)
            throws InvalidKeySpecException {
        String privateKeyPem =
                credentials.privateKey().replace("-----BEGIN PUBLIC KEY-----", "").replace("\n", "").replace("\r", "")
                           .replace("-----END PUBLIC KEY-----", "");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyPem);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
    }

    private static RSAPublicKey getPublicKey(Credentials credentials, KeyFactory keyFactory)
            throws InvalidKeySpecException {
        String publicKeyPem =
                credentials.publicKey().replace("-----BEGIN PUBLIC KEY-----", "").replace("\n", "").replace("\r", "")
                           .replace("-----END PUBLIC KEY-----", "");
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyPem);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    record Credentials(String privateKey, String publicKey) {
    }
}
