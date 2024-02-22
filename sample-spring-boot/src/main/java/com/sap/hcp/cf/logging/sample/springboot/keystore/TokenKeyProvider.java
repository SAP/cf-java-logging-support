package com.sap.hcp.cf.logging.sample.springboot.keystore;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.sap.hcp.cf.logging.sample.springboot.config.KeyStoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Provides private and public keys for JWT generation and validation from keystore. This class is an adapter between
 * {@link Algorithm} and a keystore.
 */
@Component
public class TokenKeyProvider implements RSAKeyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TokenKeyProvider.class);

    private KeyStoreConfiguration config;
    private KeyStore keyStore;

    public TokenKeyProvider(@Autowired KeyStoreConfiguration config, @Autowired ResourceLoader resources) {
        this.config = config;
        this.keyStore = initializeKeyStore(config, resources);
    }

    private static KeyStore initializeKeyStore(KeyStoreConfiguration config, ResourceLoader resources) {
        try {
            KeyStore keyStore = KeyStore.getInstance(config.getType());
            try (InputStream inputStream = getKeyStoreResource(config, resources)) {
                keyStore.load(inputStream, config.getPassword());
                LOG.debug("public-key:", Base64.getEncoder().encodeToString(
                        keyStore.getCertificate(config.getKeyAlias()).getPublicKey().getEncoded()));
                return keyStore;
            } catch (IOException | NoSuchAlgorithmException | CertificateException cause) {
                throw new RuntimeException("Cannot load token key store", cause);
            }
        } catch (java.security.KeyStoreException cause) {
            throw new RuntimeException("Cannot initialize token key store", cause);
        }
    }

    private static InputStream getKeyStoreResource(KeyStoreConfiguration config, ResourceLoader resources)
            throws IOException {
        String location = config.getLocation();
        Resource resource = resources.getResource(location);
        return resource.getInputStream();
    }

    @Override
    public RSAPublicKey getPublicKeyById(String keyId) {
        try {
            Certificate certificate = keyStore.getCertificate(keyId);
            PublicKey publicKey = certificate.getPublicKey();
            return publicKey instanceof RSAPublicKey ? (RSAPublicKey) publicKey : null;
        } catch (java.security.KeyStoreException cause) {
            LOG.info("Cannot load certificate " + keyId + " from key store " + config.getLocation() + ".", cause);
            return null;
        }
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        Key key;
        try {
            key = keyStore.getKey(config.getKeyAlias(), config.getKeyPassword());
            return key instanceof RSAPrivateKey ? (RSAPrivateKey) key : null;
        } catch (UnrecoverableKeyException | java.security.KeyStoreException | NoSuchAlgorithmException cause) {
            LOG.info("Cannot load private key from key store " + config.getLocation() + ".", cause);
            return null;
        }
    }

    @Override
    public String getPrivateKeyId() {
        return config.getKeyAlias();
    }
}
