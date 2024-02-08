package com.sap.hcp.cf.logging.servlet.dynlog;

import com.sap.hcp.cf.logging.common.helper.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class PublicKeyReader {

    private final static Logger LOGGER = LoggerFactory.getLogger(PublicKeyReader.class);

    private PublicKeyReader() {
    }

    public static RSAPublicKey readPublicKey(Environment environment)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        String pemKey = environment.getVariable("DYN_LOG_LEVEL_KEY");

        if (pemKey == null) {
            LOGGER.info("DYN_LOG_LEVEL_KEY not found in environment");
            return null;
        } else {
            pemKey = pemKey.replace("-----BEGIN PUBLIC KEY-----", "");
            pemKey = pemKey.replace("\n", "");
            pemKey = pemKey.replace("\r", "");
            pemKey = pemKey.replace("-----END PUBLIC KEY-----", "");

            byte[] pubKeyBytes = Base64.getDecoder().decode(pemKey);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(spec);
        }
    }
}
