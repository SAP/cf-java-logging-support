package com.sap.hcp.cf.logging.servlet.dynlog;

import com.sap.hcp.cf.logging.common.helper.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

public class DynLogEnvironment implements DynamicLogLevelConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynLogEnvironment.class);
    private final RSAPublicKey rsaPublicKey;
    private final String dynLogHeaderKey;

    public DynLogEnvironment() {
        this(new Environment());
    }

    DynLogEnvironment(Environment environment) {
        String header = environment.getVariable("DYN_LOG_HEADER");
        if (header != null) {
            dynLogHeaderKey = header;
            LOGGER.info("The header key used to retrieve the dynamic log level token has been set to {}", header);
        } else {
            dynLogHeaderKey = "SAP-LOG-LEVEL";
            LOGGER.info(
                    "The header key used to retrieve the dynamic log level token has been set to the default value: {}",
                    dynLogHeaderKey);
        }

        RSAPublicKey tempKey = null;
        try {
            tempKey = PublicKeyReader.readPublicKey(environment);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            LOGGER.error("Could not read RSAPublicKey from environment", e);
        } finally {
            rsaPublicKey = tempKey;
        }
    }

    @Override
    public RSAPublicKey getRsaPublicKey() {
        return rsaPublicKey;
    }

    @Override
    public String getDynLogHeaderKey() {
        return dynLogHeaderKey;
    }
}
