package com.sap.hcp.cf.logging.servlet.dynlog.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sap.hcp.cf.logging.common.helper.Environment;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelProvider;
import com.sap.hcp.cf.logging.servlet.dynlog.jwt.spi.JwtAlgorithmProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.ServiceLoader;

import static java.util.function.Predicate.not;

/**
 * A {@link DynamicLogLevelProvider} intercepting requests and extracting the dynamic log level configuration from a JWT
 * in the request headers. It requires a public key to verify the JWT which can be provided as environment variable or
 * via SPI {@link JwtAlgorithmProvider}.
 */
public class JwtDynamicLogLevelProvider implements DynamicLogLevelProvider {

    // Environment variable to contain the HTTP header name to use for configuration.
    private static final String ENV_DYN_LOG_HEADER = "DYN_LOG_HEADER";
    // Environment variable to contain the public key for JWT verification.
    private static final String ENV_DYN_LOG_PUBLIC_KEY = "DYN_LOG_LEVEL_KEY";
    private static final String DEFAULT_DYN_LOG_HEADER = "SAP-LOG-LEVEL";
    private static final String JWT_LEVEL = "level";
    private static final String JWT_PACKAGES = "packages";

    private static final Logger LOG = LoggerFactory.getLogger(JwtDynamicLogLevelProvider.class);
    private final String headerName;
    private final JWTVerifier verifier;

    public JwtDynamicLogLevelProvider() {
        this(new Environment());
    }

    JwtDynamicLogLevelProvider(Environment environment) {
        Creator creator = new Creator(environment);
        this.headerName = creator.getHeaderName();
        this.verifier = creator.getVerifier().orElse(null);
    }

    // for testing only
    String getHeaderName() {
        return headerName;
    }

    // for testing only
    JWTVerifier getVerifier() {
        return verifier;
    }

    @Override
    public DynamicLogLevelConfiguration apply(HttpServletRequest httpServletRequest) {
        if (verifier == null) {
            return DynamicLogLevelConfiguration.EMPTY;
        }
        String header = httpServletRequest.getHeader(headerName);
        if (header == null || header.isEmpty()) {
            return DynamicLogLevelConfiguration.EMPTY;
        }
        DecodedJWT jwt = decode(header);
        return createConfig(jwt);
    }

    private static DynamicLogLevelConfiguration createConfig(DecodedJWT jwt) {
        if (jwt == null) {
            return DynamicLogLevelConfiguration.EMPTY;
        }
        return new DynamicLogLevelConfiguration(getClaimAsString(JWT_LEVEL, jwt), getClaimAsString(JWT_PACKAGES, jwt));
    }

    private static String getClaimAsString(String name, DecodedJWT jwt) {
        Claim claim = jwt.getClaim(name);
        return claim != null ? claim.asString() : null;
    }

    private DecodedJWT decode(String header) {
        try {
            return verifier.verify(header);
        } catch (JWTVerificationException e) {
            LOG.debug("Token verification failed", e);
            return null;
        }
    }

    private static class Creator {

        private final Environment environment;

        private Creator(Environment environment) {
            this.environment = environment;
        }

        private String getHeaderName() {
            String headerName = environment.getVariable(ENV_DYN_LOG_HEADER);
            if (headerName != null && !headerName.isEmpty()) {
                LOG.info("The HTTP header name to retrieve the dynamic log level token has been set to {}.",
                         headerName);
                return headerName;
            } else {
                LOG.info(
                        "The HTTP header name to retrieve the dynamic log level token has been set to the default value {}.",
                        DEFAULT_DYN_LOG_HEADER);
                return DEFAULT_DYN_LOG_HEADER;
            }
        }

        private Optional<JWTVerifier> getVerifier() {
            return loadJwtAlgorithm().or(() -> getRawPem().map(Creator::extractPemKey).filter(not(String::isBlank))
                                                          .flatMap(this::parsePublicKey)
                                                          .map(k -> Algorithm.RSA256(k, null)))
                                     .map(algorithm -> JWT.require(algorithm).build());
        }

        private Optional<Algorithm> loadJwtAlgorithm() {
            return ServiceLoader.load(JwtAlgorithmProvider.class).stream().map(ServiceLoader.Provider::get)
                                .map(JwtAlgorithmProvider::get).findFirst();
        }

        private Optional<String> getRawPem() {
            String rawPem = environment.getVariable(ENV_DYN_LOG_PUBLIC_KEY);
            if (rawPem == null || rawPem.isEmpty()) {
                LOG.info("No valid {} found in environment.", ENV_DYN_LOG_PUBLIC_KEY);
                return Optional.empty();
            }
            return Optional.of(rawPem);
        }

        private Optional<RSAPublicKey> parsePublicKey(String pem) {
            byte[] pubKeyBytes;
            try {
                pubKeyBytes = Base64.getDecoder().decode(pem);
            } catch (IllegalArgumentException cause) {
                LOG.info("Cannot decode public key.", cause);
                return Optional.empty();
            }
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            Optional<KeyFactory> keyFactory = getRsaKeyFactory();

            return keyFactory.map(kf -> generatePublicKey(kf, keySpec));
        }

        private static RSAPublicKey generatePublicKey(KeyFactory kf, X509EncodedKeySpec keySpec) {
            try {
                return (RSAPublicKey) kf.generatePublic(keySpec);
            } catch (InvalidKeySpecException cause) {
                LOG.info("Cannot parse public RSA key.", cause);
                return null;
            } catch (ClassCastException cause) {
                LOG.info("Cannot cast public key to RSA key", cause);
                return null;
            }
        }

        private static Optional<KeyFactory> getRsaKeyFactory() {
            try {
                return Optional.of(KeyFactory.getInstance("RSA"));
            } catch (NoSuchAlgorithmException e) {
                LOG.warn("Unable to instantiate RSA key factory");
                return Optional.empty();
            }
        }

        private static String extractPemKey(String pem) {
            String result = pem.replace("-----BEGIN PUBLIC KEY-----", "");
            result = result.replace("\n", "");
            result = result.replace("\r", "");
            result = result.replace("-----END PUBLIC KEY-----", "");
            return result;
        }
    }
}
