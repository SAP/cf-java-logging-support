package com.sap.hcp.cf.logging.servlet.dynlog;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class DynamicLogLevelProcessorTest {

    private DynamicLogLevelProcessor processor;

    private String token;

    private KeyPair keyPair;

    private static RSAPublicKey getRSAPublicKey(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        if (publicKey instanceof RSAPublicKey) {
            return (RSAPublicKey) publicKey;
        }
        return null;
    }

    @BeforeEach
    public void setup() throws NoSuchAlgorithmException, NoSuchProviderException, DynamicLogLevelException {
        this.keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        Date issuedAt = new Date();
        Date expiresAt = new Date(new Date().getTime() + 10000);
        this.token = TokenCreator.createToken(keyPair, "issuer", issuedAt, expiresAt, "TRACE", "myPrefix");
        this.processor = new DynamicLogLevelProcessor(getRSAPublicKey(keyPair));
    }

    @AfterEach
    public void removeDynamicLogLevelFromMDC() {
        processor.removeDynamicLogLevelFromMDC();
    }

    @Test
    public void copiesDynamicLogLevelToMDC() throws Exception {
        processor.copyDynamicLogLevelToMDC(token);
        assertThat(MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY)).isEqualTo("TRACE");
    }

    @Test
    public void deletesDynamicLogLevelFromMDC() throws Exception {
        processor.copyDynamicLogLevelToMDC(token);
        processor.removeDynamicLogLevelFromMDC();
        assertNull(MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
    }

    @Test
    public void copiesDynamicLogPackagesToMDC() throws Exception {
        processor.copyDynamicLogLevelToMDC(token);
        assertThat(MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES)).isEqualTo("myPrefix");
    }

    @Test
    public void doesNotCopyDynamicLogLevelOnInvalidJwt() throws Exception {
        KeyPair invalidKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        new DynamicLogLevelProcessor(getRSAPublicKey(invalidKeyPair)).copyDynamicLogLevelToMDC(token);
        assertNull(MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
    }

    @Test
    public void doesNotCopyDynamicLogLevelOnCustomException() throws Exception {
        DynamicLogLevelProcessor myProcessor = new DynamicLogLevelProcessor(getRSAPublicKey(keyPair)) {
            @Override
            protected void processJWT(DecodedJWT jwt) throws DynamicLogLevelException {
                throw new DynamicLogLevelException("Always fail in this test-case.");
            }
        };
        myProcessor.copyDynamicLogLevelToMDC(token);
        assertNull(MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
    }
}
