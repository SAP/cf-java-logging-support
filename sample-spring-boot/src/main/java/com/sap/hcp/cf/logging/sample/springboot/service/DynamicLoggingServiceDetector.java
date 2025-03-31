package com.sap.hcp.cf.logging.sample.springboot.service;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

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
        this(new CfEnv());
    }

    DynamicLoggingServiceDetector(CfEnv cfEnv) {
        List<CfService> services = cfEnv.findServicesByName(SERVICE_NAME);
        if (services != null) {
            for (CfService service: services) {
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    RSAPrivateKey privateKeyCandidate = getPrivateKey(service.getCredentials(), keyFactory);
                    RSAPublicKey publicKeyCandidate = getPublicKey(service.getCredentials(), keyFactory);
                    if (privateKeyCandidate != null && publicKeyCandidate != null) {
                        this.privateKey = privateKeyCandidate;
                        this.publicKey = publicKeyCandidate;
                    }
                } catch (Exception ex) {
                    LOG.warn(
                            "Found service binding to " + service.getName() + "(" + service.getLabel() + "). But could not read RSA keys.",
                            ex);
                }
            }
        }
    }

    private static RSAPrivateKey getPrivateKey(CfCredentials credentials, KeyFactory keyFactory)
            throws InvalidKeySpecException {
        String privateKeyRaw = credentials.getString("privateKey");
        String privateKeyPem =
                privateKeyRaw.replace("-----BEGIN PUBLIC KEY-----", "").replace("\n", "").replace("\r", "")
                             .replace("-----END PUBLIC KEY-----", "");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyPem);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
    }

    private static RSAPublicKey getPublicKey(CfCredentials credentials, KeyFactory keyFactory)
            throws InvalidKeySpecException {
        String publicKeyRaw = credentials.getString("publicKey");
        String publicKeyPem = publicKeyRaw.replace("-----BEGIN PUBLIC KEY-----", "").replace("\n", "").replace("\r", "")
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
}
