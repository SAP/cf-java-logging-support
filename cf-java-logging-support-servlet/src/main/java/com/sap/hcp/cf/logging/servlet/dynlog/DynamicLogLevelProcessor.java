package com.sap.hcp.cf.logging.servlet.dynlog;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides a mechanism that reads a token from an
 * HTTP-request-header. If this token is provided and does contain a correct
 * signature, valid timestamps and a log-level-value, the log-level for the
 * thread triggered by this request will be changed to the provided value.
 * 
 * You can extend this processor to extract custom claims from the JWT. Override
 * {@link DynamicLogLevelProcessor#processJWT(DecodedJWT)} for this.
 */
public class DynamicLogLevelProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(DynamicLogLevelProcessor.class);
    private static final List<String> ALLOWED_DYNAMIC_LOGLEVELS = Arrays.asList("TRACE", "DEBUG", "INFO", "WARN",
                                                                                "ERROR");
    private final TokenDecoder tokenDecoder;

    /**
     * @deprecated Use
     *             {@link DynamicLogLevelProcessor#DynamicLogLevelProcessor(RSAPublicKey)}
     *             instead.
     * @param dynLogConfig
     *            the {@link DynamicLogLevelConfiguration} to read the public RSA key for
     *            JWT validation from.
     */
    @Deprecated
    public DynamicLogLevelProcessor(DynamicLogLevelConfiguration dynLogConfig) {
        this(dynLogConfig.getRsaPublicKey());
    }

    public DynamicLogLevelProcessor(RSAPublicKey publicJwtKey) {
        this.tokenDecoder = new TokenDecoder(publicJwtKey);
    }

    /**
     * Decodes and validate the JWT. Configures the dynamic log levels by
     * setting the corresponding fields in the MDC.
     * 
     * @param logLevelToken
     *            the HTTP Header containing the JWT for dynamic log levels
     */
    public void copyDynamicLogLevelToMDC(String logLevelToken) {
        if (logLevelToken == null) {
            return;
        } else {
            try {
                DecodedJWT jwt = tokenDecoder.validateAndDecodeToken(logLevelToken);
                processJWT(jwt);
            } catch (DynamicLogLevelException cause) {
                LOGGER.warn("DynamicLogLevelProcessor could not write dynamic log level to MDC", cause);
            }
        }
    }

    /**
     * Extracts the relevant claims for dynamic log level configuration from the
     * decoded token. In case of faulty content, i.e. unknown log level a
     * {@link DynamicLogLevelException} is thrown. You can override this method
     * to implement own interaction with the JWT, e.g. extraction and validation
     * of additional claims.
     * 
     * @param jwt
     *            the decoded JWT from the HTTP header
     * @throws DynamicLogLevelException
     *             if validation of JWT claims fail
     */
    protected void processJWT(DecodedJWT jwt) throws DynamicLogLevelException {
        String dynamicLogLevel = jwt.getClaim("level").asString();
        String packages = jwt.getClaim("packages").asString();
        if (ALLOWED_DYNAMIC_LOGLEVELS.contains(dynamicLogLevel)) {
            MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, dynamicLogLevel);
            MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, packages);
        } else {
            throw new DynamicLogLevelException("Dynamic Log-Level [" + dynamicLogLevel +
                                               "] provided in header is not valid. Allowed Values are " +
                                               ALLOWED_DYNAMIC_LOGLEVELS.toString());
        }
    }

    /**
     * Resets the current dynamic log level configuration by removing the
     * corresponding fields from the MDC. This needs to be called to remove the
     * changed log level configuration.
     */
    public void removeDynamicLogLevelFromMDC() {
        MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
    }
}
