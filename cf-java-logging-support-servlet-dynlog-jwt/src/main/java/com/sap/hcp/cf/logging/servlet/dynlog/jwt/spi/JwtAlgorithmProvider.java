package com.sap.hcp.cf.logging.servlet.dynlog.jwt.spi;

import com.auth0.jwt.algorithms.Algorithm;

import java.util.function.Supplier;

/**
 * The {@link JwtAlgorithmProvider} can be used to register a service that provides an {@link Algorithm}. This algorithm
 * will be used for JWT verification of the dynamic log level token. Use this interface to provide the rsa public key in
 * another way than as environment variable.
 */
public interface JwtAlgorithmProvider extends Supplier<Algorithm> {
}
