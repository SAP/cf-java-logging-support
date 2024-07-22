package com.sap.hcp.cf.logging.sample.springboot.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.sap.hcp.cf.logging.sample.springboot.config.TokenDefaultsConfiguration;
import com.sap.hcp.cf.logging.sample.springboot.keystore.TokenKeyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenGenerator {

    private TokenKeyProvider keyProvider;
    private TokenDefaultsConfiguration defaults;

    public TokenGenerator(@Autowired TokenKeyProvider keyProvider, @Autowired TokenDefaultsConfiguration defaults) {
        this.keyProvider = keyProvider;
        this.defaults = defaults;
    }

    public String create(String logLevel, Optional<String> packages, Instant expiresAt, Instant issuedAt) {
        Algorithm algorithm = Algorithm.RSA256(keyProvider);
        Builder jwtBuilder = JWT.create().withIssuer(defaults.getIssuer()).withIssuedAt(Date.from(issuedAt))
                                .withExpiresAt(Date.from(expiresAt)).withClaim("level", logLevel);
        packages.ifPresent(p -> jwtBuilder.withClaim("packages", p));
        return jwtBuilder.sign(algorithm);

    }

}
