package com.sap.hcp.cf.logging.servlet.dynlog;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;

public class TokenDecoder {

    private JWTVerifier verifier;

    public TokenDecoder(RSAPublicKey key) {
        verifier = JWT.require(Algorithm.RSA256((RSAPublicKey) key, null)).build();
    }

    /**
     * This method validates if a token has a valid signature as well as a valid timestamp and returns the decoded
     * token
     *
     * @throws DynamicLogLevelException
     */
    public DecodedJWT validateAndDecodeToken(String token) throws DynamicLogLevelException {
        try {
            DecodedJWT jwt = verifier.verify(token);
            Date exp = jwt.getExpiresAt();
            Date iat = jwt.getIssuedAt();
            Date now = new Date();

            if (exp != null && iat != null && now.after(iat) && now.before(exp)) {
                return jwt;
            } else {
                throw new DynamicLogLevelException(
                        "Token provided to dynamically change the log-level on thread-level is outdated");
            }
        } catch (JWTVerificationException e) {
            // Exception is not attached to avoid logging of JWT token
            throw new DynamicLogLevelException("Token could not be verified");
        }
    }
}
