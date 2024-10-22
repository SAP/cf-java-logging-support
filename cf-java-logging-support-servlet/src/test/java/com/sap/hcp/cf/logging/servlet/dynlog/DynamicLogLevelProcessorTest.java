package com.sap.hcp.cf.logging.servlet.dynlog;

import static org.junit.Assert.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;

@RunWith(MockitoJUnitRunner.class)
public class DynamicLogLevelProcessorTest extends Mockito {

    private DynamicLogLevelProcessor processor;

    private String token;

    private KeyPair keyPair;

    @Before
    public void setup() throws NoSuchAlgorithmException, NoSuchProviderException, DynamicLogLevelException {
        this.keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        Date issuedAt = new Date();
        Date expiresAt = new Date(new Date().getTime() + 10000);
        this.token = TokenCreator.createToken(keyPair, "issuer", issuedAt, expiresAt, "TRACE", "myPrefix");
        this.processor = new DynamicLogLevelProcessor(getRSAPublicKey(keyPair));
    }

    private static RSAPublicKey getRSAPublicKey(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        if (publicKey instanceof RSAPublicKey) {
            return (RSAPublicKey) publicKey;
        }
        return null;
    }

    @After
    public void removeDynamicLogLevelFromMDC() {
        processor.removeDynamicLogLevelFromMDC();
    }

    @Test
    public void copiesDynamicLogLevelToMDC() throws Exception {
        processor.copyDynamicLogLevelToMDC(token);
        assertEquals("TRACE", MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
    }

    @Test
    public void deletesDynamicLogLevelFromMDC() throws Exception {
        processor.copyDynamicLogLevelToMDC(token);
        processor.removeDynamicLogLevelFromMDC();
        assertEquals(null, MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
    }

    @Test
    public void copiesDynamicLogPackagesToMDC() throws Exception {
        processor.copyDynamicLogLevelToMDC(token);
        assertEquals("myPrefix", MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES));
    }

    @Test
    public void doesNotCopyDynamicLogLevelOnInvalidJwt() throws Exception {
        KeyPair invalidKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        new DynamicLogLevelProcessor(getRSAPublicKey(invalidKeyPair)).copyDynamicLogLevelToMDC(token);
        assertEquals(null, MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
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
        assertEquals(null, MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY));
    }
}
