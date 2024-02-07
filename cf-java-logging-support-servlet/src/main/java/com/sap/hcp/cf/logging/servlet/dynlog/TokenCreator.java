package com.sap.hcp.cf.logging.servlet.dynlog;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.lang3.StringUtils;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * This class provides a small example application to generate JWT tokens for
 * the dynamic log level feature. The input data is hard-coded in the
 * source-code. Please change the value for your use-case.
 *
 */
public class TokenCreator {

    private static final List<String> ALLOWED_DYNAMIC_LOGLEVELS = Arrays.asList("TRACE", "DEBUG", "INFO", "WARN",
                                                                                "ERROR");

    /**
     * Run this application locally in order to generate valid dynamic log level
     * JWT tokens which enable you to change the log level threshold on your
     * CF-Application for a single thread.
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException,
                                           DynamicLogLevelException, InvalidKeySpecException {

        /*
         * PLEASE PROVIDE THIS INFORMATION ***********************************
         */
        // Replace with email address
        String issuer = "firstname.lastname@sap.com";
        // Replace with the log level that should be transmitted via the token
        // Valid log level thresholds are:
        // "TRACE", "DEBUG", "INFO", "WARN", "ERROR"
        String level = "TRACE";
        // Replace with the packages that should be transmitted via the token
        // Multiple packages should be separated by a comma.
        String packages = "";
        // Set a validity period in days
        long validityPeriodInDays = 2;
        // If available provide Base64 encoded private key here:
        String privateKey = "";
        // If available provide Base64 encoded private key here:
        String publicKey = "";
        // (If no keys are provided, new keys will be generated)
        /*
         * ********************************************************************
         */

        KeyPair keyPair;

        if (StringUtils.isNotBlank(privateKey)) {
            keyPair = new KeyPair(publicKeyConverter(publicKey), privateKeyConverter(privateKey));
        }

        else {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            keyPair = keyGen.generateKeyPair();
            // keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        }
        privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        Date issuedAt = new Date();
        Date expiresAt = new Date(new Date().getTime() + validityPeriodInDays * 86400000);
        String token = TokenCreator.createToken(keyPair, issuer, issuedAt, expiresAt, level, packages);

        System.out.println("You successfully created a dynamic log level token with log level " + level +
                           " and packages " + packages + "!");
        System.out.println();
        System.out.println("Your private key is:");
        System.out.println(privateKey);
        System.out.println("Your public key is:");
        System.out.println(publicKey);
        System.out.println("Your JWT token with log level " + level + " is:");
        System.out.println(token);
        System.out.println();
        System.out.println("Please copy and save token and keys for later usage. The JWT token can now be written");
        System.out.println("to an HTTP header in order to change the corresponding request's log level to " + level);
        System.out.println("For token validation, the public key must be added to the environment of the application.");
        System.out.println("In order to generate a new token with specific keys, the variables privateKey and publicKey");
        System.out.println("can be instantiated with these keys");

    }

    public static String createToken(KeyPair keyPair, String issuer, Date issuedAt, Date expiresAt, String level,
                                     String packages) throws NoSuchAlgorithmException, NoSuchProviderException,
                                                      DynamicLogLevelException {
        Algorithm rsa256 = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        if (ALLOWED_DYNAMIC_LOGLEVELS.contains(level)) {
            Builder builder = JWT.create().withIssuer(issuer).//
                                 withIssuedAt(issuedAt). //
                                 withExpiresAt(expiresAt).//
                                 withClaim("level", level);
            builder = StringUtils.isNotBlank(packages) ? builder.withClaim("packages", packages) : builder;
            return builder.withClaim("packages", packages).sign(rsa256);
        } else {
            throw new DynamicLogLevelException("Dynamic Log-Level [" + level +
                                               "] provided in header is not valid. Allowed Values are " +
                                               ALLOWED_DYNAMIC_LOGLEVELS.toString());
        }
    }

    private static RSAPublicKey publicKeyConverter(String pemKey) throws NoSuchAlgorithmException,
                                                                  InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(pemKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    private static RSAPrivateKey privateKeyConverter(String pemKey) throws NoSuchAlgorithmException,
                                                                    InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(pemKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

}
