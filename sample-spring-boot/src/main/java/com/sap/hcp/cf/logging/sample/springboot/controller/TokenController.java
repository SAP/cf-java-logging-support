package com.sap.hcp.cf.logging.sample.springboot.controller;

import com.sap.hcp.cf.logging.sample.springboot.config.TokenDefaultsConfiguration;
import com.sap.hcp.cf.logging.sample.springboot.service.TokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * This controller provides an endpoint to create new JWT tokens. These token can be used as headers of HTTP request to
 * dynamically switch the log level.
 */
@RestController
public class TokenController {

    private static final Logger LOG = LoggerFactory.getLogger(TokenController.class);

    private Clock clock;
    private TokenGenerator generator;
    private TokenDefaultsConfiguration defaults;

    public TokenController(@Autowired Clock clock, @Autowired TokenGenerator generator,
                           @Autowired TokenDefaultsConfiguration defaults) {
        this.clock = clock;
        this.generator = generator;
        this.defaults = defaults;
    }

    /**
     * Return a JWT for changing the log level dynamically. It uses the keys provided by the keystore to sign the JWT.
     *
     * @param logLevel
     *         the log level to use when JWT is applied
     * @param expiresAtParam
     *         (optional) the expiration of the JWT in epoch milliseconds
     * @param packages
     *         (optional) the comma-separated list of packages for which the log levels should be changed
     * @return the signed JWT to be used as HTTP header
     */
    @GetMapping("/token/{logLevel}")
    public String createToken(@PathVariable("logLevel") String logLevel,
                              @RequestParam(name = "exp") Optional<Long> expiresAtParam,
                              @RequestParam(name = "p") Optional<String> packages) {
        Instant expiresAt = getExpiryOrDefault(expiresAtParam);
        Instant issuedAt = clock.instant();
        return generator.create(logLevel, packages, expiresAt, issuedAt);
    }

    private Instant getExpiryOrDefault(Optional<Long> expiresAtParam) {
        if (expiresAtParam.isPresent()) {
            Instant result = expiresAtParam.map(Instant::ofEpochMilli).get();
            LOG.debug("Using user provided expiration at {}.", result);
            return result;
        }
        Instant result = clock.instant().plus(defaults.getExpiration());
        LOG.debug("Using default expiration to calculate {}.", result);
        return result;

    }

}
