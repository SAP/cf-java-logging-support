package com.sap.hcp.cf.logging.sample.springboot.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.sap.hcp.cf.logging.sample.springboot.config.TokenDefaultsConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.jwt.spi.JwtAlgorithmProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.ServiceLoader;

@Service
public class TokenGenerator {

    private final TokenDefaultsConfiguration defaults;
    private final Algorithm algorithm;

    public TokenGenerator(@Autowired TokenDefaultsConfiguration defaults) {
        this.defaults = defaults;
        this.algorithm = ServiceLoader.load(JwtAlgorithmProvider.class).findFirst().map(JwtAlgorithmProvider::get)
                                      .orElseThrow(() -> new IllegalStateException(
                                              "Cannot initialize TokenGenerator, no JWT algorithm found."));
    }

    public String create(String logLevel, Optional<String> packages, Instant expiresAt, Instant issuedAt) {
        Builder jwtBuilder = JWT.create().withIssuer(defaults.getIssuer()).withIssuedAt(Date.from(issuedAt))
                                .withExpiresAt(Date.from(expiresAt)).withClaim("level", logLevel);
        packages.ifPresent(p -> jwtBuilder.withClaim("packages", p));
        return jwtBuilder.sign(algorithm);

    }

}
