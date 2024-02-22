package com.sap.hcp.cf.logging.servlet.dynlog;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenDecoderTest {

    private String token;
    private KeyPair validKeyPair;
    private KeyPair invalidKeyPair;

    @BeforeEach
    public void setup() throws NoSuchAlgorithmException, NoSuchProviderException, DynamicLogLevelException {
        validKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        invalidKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        Date issuedAt = new Date();
        Date expiresAt = new Date(new Date().getTime() + 10000);
        token = TokenCreator.createToken(validKeyPair, "issuer", issuedAt, expiresAt, "TRACE", "myPrefix");
    }

    @Test
    public void testTokenContent() throws Exception {
        TokenDecoder tokenDecoder = new TokenDecoder((RSAPublicKey) validKeyPair.getPublic());
        DecodedJWT jwt = tokenDecoder.validateAndDecodeToken(token);
        assertThat(jwt.getClaim("level").asString()).isEqualTo("TRACE");
        assertThat(jwt.getClaim("packages").asString()).isEqualTo("myPrefix");
    }

    @Test
    public void testInvalidPublicKey() throws Exception {
        assertThrows(DynamicLogLevelException.class, () -> {
            TokenDecoder tokenDecoder = new TokenDecoder((RSAPublicKey) invalidKeyPair.getPublic());
            tokenDecoder.validateAndDecodeToken(token);
        });
    }
}
