package com.sap.hcp.cf.logging.servlet.dynlog;

import jakarta.servlet.http.HttpServletRequest;

import java.security.interfaces.RSAPublicKey;

@FunctionalInterface
public interface DynamicLogLevelConfiguration {

    RSAPublicKey getRsaPublicKey();

    default String getDynLogHeaderKey() {
        return "SAP-LOG-LEVEL";
    };

    default String getDynLogHeaderValue(HttpServletRequest httpRequest) {
        return httpRequest.getHeader(getDynLogHeaderKey());
    }

}
