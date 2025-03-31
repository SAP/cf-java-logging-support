package com.sap.hcp.cf.logging.sample.springboot.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.sap.hcp.cf.logging.servlet.dynlog.jwt.spi.JwtAlgorithmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class DynamicLogLevelAlgorithmProvider implements JwtAlgorithmProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicLogLevelAlgorithmProvider.class);

    @Override
    public Algorithm get() {
        KeyPair keyPair = getKeyPair();
        return keyPair == null
                ? null
                : Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
    }

    public static KeyPair getKeyPair() {
        return KeyPairHolder.keyPair;
    }

    private static class KeyPairHolder {
        static final KeyPair keyPair = createKeyPair();

        private static KeyPair createKeyPair() {
            DynamicLoggingServiceDetector serviceDetector = new DynamicLoggingServiceDetector();
            if (serviceDetector.getPrivateKey() != null && serviceDetector.getPublicKey() != null) {
                return new KeyPair(serviceDetector.getPublicKey(), serviceDetector.getPrivateKey());
            } else {
                try {
                    LOG.debug("Did not find a service binding, generating own key pair.");
                    return KeyPairGenerator.getInstance("RSA").generateKeyPair();
                } catch (Exception ex) {
                    LOG.warn("Cannot create own key pair.", ex);
                }
            }
            return null;
        }
    }

}
