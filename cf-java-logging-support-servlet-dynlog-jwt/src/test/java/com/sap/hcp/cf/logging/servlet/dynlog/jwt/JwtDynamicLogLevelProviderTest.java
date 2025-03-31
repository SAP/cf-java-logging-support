package com.sap.hcp.cf.logging.servlet.dynlog.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sap.hcp.cf.logging.common.helper.Environment;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import static com.sap.hcp.cf.logging.servlet.dynlog.jwt.PemUtils.createPublicKeyPem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtDynamicLogLevelProviderTest {

    @Mock
    private Environment environment;

    @Mock
    private HttpServletRequest request;

    @Test
    void usesDefaultHeaderNameOnMissingEnv() {
        assertThat(new JwtDynamicLogLevelProvider(environment).getHeaderName()).isEqualTo("SAP-LOG-LEVEL");
    }

    @Test
    void usesDefaultHeaderNameOnEmptyEnv() {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn("");

        assertThat(new JwtDynamicLogLevelProvider(environment).getHeaderName()).isEqualTo("SAP-LOG-LEVEL");
    }

    @Test
    void usesCustomHeaderName() {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn("x-dynlog-header");

        assertThat(new JwtDynamicLogLevelProvider(environment).getHeaderName()).isEqualTo("x-dynlog-header");
    }

    @Test
    void noVerifierOnMissingPublicKey() {
        assertThat(new JwtDynamicLogLevelProvider(environment).getVerifier()).isNull();
    }

    @Test
    void noVerifierOnEmptyPublicKey() {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn("");
        assertThat(new JwtDynamicLogLevelProvider(environment).getVerifier()).isNull();
    }

    @Test
    void noVerifierNonsensePublicKey() throws NoSuchAlgorithmException {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn("just some nonsense");
        assertThat(new JwtDynamicLogLevelProvider(environment).getVerifier()).isNull();
    }

    @Test
    void noVerifierInvalidPublicKey() throws NoSuchAlgorithmException {
        String validKey = createPublicKeyPem(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic());
        String invalidKey = Arrays.stream(validKey.split("\n")).skip(2).collect(Collectors.joining("\n"));
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn(invalidKey);
        assertThat(new JwtDynamicLogLevelProvider(environment).getVerifier()).isNull();
    }

    @Test
    void verifierOnValidPublicKey() throws NoSuchAlgorithmException {
        String validKey = createPublicKeyPem(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic());
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn(validKey);
        assertThat(new JwtDynamicLogLevelProvider(environment).getVerifier()).isNotNull();
    }

    @Test
    void noDynamicLogLevelWithoutConfiguration() {
        assertThat(new JwtDynamicLogLevelProvider().apply(request)).isEqualTo(DynamicLogLevelConfiguration.EMPTY);
    }

    @Test
    void createsDynamicLogLevelWithValidConfiguration() throws NoSuchAlgorithmException {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn(createPublicKeyPem(keyPair.getPublic()));
        Algorithm rsa256 = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        String jwt = JWT.create().withIssuer("createsDynamicLogLevelWithValidConfiguration")
                        .withIssuedAt(Date.from(Instant.now()))
                        .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES))).withClaim("level", "DEBUG")
                        .withClaim("packages", "com.sap.hcp").sign(rsa256);
        when(request.getHeader("SAP-LOG-LEVEL")).thenReturn(jwt);

        DynamicLogLevelConfiguration logLevelConfiguration = new JwtDynamicLogLevelProvider(environment).apply(request);

        assertThat(logLevelConfiguration).extracting(DynamicLogLevelConfiguration::level).isEqualTo("DEBUG");
        assertThat(logLevelConfiguration).extracting(DynamicLogLevelConfiguration::packages).isEqualTo("com.sap.hcp");
    }

    @Test
    void emptyOnExpiredJwt() throws NoSuchAlgorithmException {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn(createPublicKeyPem(keyPair.getPublic()));
        Algorithm rsa256 = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        String jwt = JWT.create().withIssuer("createsDynamicLogLevelWithValidConfiguration")
                        .withIssuedAt(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                        .withExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)))
                        .withClaim("level", "DEBUG").withClaim("packages", "com.sap.hcp").sign(rsa256);
        when(request.getHeader("SAP-LOG-LEVEL")).thenReturn(jwt);

        assertThat(new JwtDynamicLogLevelProvider(environment).apply(request)).isEqualTo(
                DynamicLogLevelConfiguration.EMPTY);
    }

    @Test
    void emptyOnWrongSecrets() throws NoSuchAlgorithmException {
        when(environment.getVariable("DYN_LOG_HEADER")).thenReturn(null);
        KeyPair validKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        when(environment.getVariable("DYN_LOG_LEVEL_KEY")).thenReturn(createPublicKeyPem(validKeyPair.getPublic()));
        KeyPair invalidKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        Algorithm rsa256 = Algorithm.RSA256((RSAPublicKey) invalidKeyPair.getPublic(),
                                            (RSAPrivateKey) invalidKeyPair.getPrivate());
        String jwt = JWT.create().withIssuer("createsDynamicLogLevelWithValidConfiguration")
                        .withIssuedAt(Date.from(Instant.now()))
                        .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES))).withClaim("level", "DEBUG")
                        .withClaim("packages", "com.sap.hcp").sign(rsa256);
        when(request.getHeader("SAP-LOG-LEVEL")).thenReturn(jwt);

        assertThat(new JwtDynamicLogLevelProvider(environment).apply(request)).isEqualTo(
                DynamicLogLevelConfiguration.EMPTY);
    }

}
