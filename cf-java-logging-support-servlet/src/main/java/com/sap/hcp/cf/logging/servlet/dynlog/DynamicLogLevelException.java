package com.sap.hcp.cf.logging.servlet.dynlog;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.ServletException;

public class DynamicLogLevelException extends ServletException {

    private static final long serialVersionUID = 1L;

    public DynamicLogLevelException(String message, JWTVerificationException cause) {
        super(message, cause);
    }

    public DynamicLogLevelException(String message) {
        super(message);
    }

}
