package com.sap.hcp.cf.logging.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.helper.Environment;

public class LogOptionalFieldsSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogOptionalFieldsSettings.class);

    private final boolean logSensitiveConnectionData;
    private final boolean logRemoteUserField;
    private final boolean logRefererField;
    private final boolean logSslHeaders;

    public LogOptionalFieldsSettings(String invokingClass) {
        this(new Environment(), invokingClass);
    }

    LogOptionalFieldsSettings(Environment environment, String invokingClass) {
        logSensitiveConnectionData = readEnvironmentVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA, environment,
                                                             invokingClass);
        logRemoteUserField = readEnvironmentVariable(Environment.LOG_REMOTE_USER, environment, invokingClass);
        logRefererField = readEnvironmentVariable(Environment.LOG_REFERER, environment, invokingClass);
        logSslHeaders = readEnvironmentVariable(Environment.LOG_SSL_HEADERS, environment, invokingClass);
    }

    private static boolean readEnvironmentVariable(String environmentVariableKey, Environment environment,
                                                   String invokingClass) {
        String tempEnvVariable = environment.getVariable(environmentVariableKey);

        if (tempEnvVariable == null) {
            LOGGER.trace("Logging remote IPs is DEACTIVATED by default for {}.", invokingClass);
            return false;
        }

        if ("true".equalsIgnoreCase(tempEnvVariable)) {
            LOGGER.trace("Logging field {} has been ACTIVATED via environment variable for {}", environmentVariableKey,
                         invokingClass);
            return true;
        }

        if ("false".equalsIgnoreCase(tempEnvVariable)) {
            LOGGER.trace("Logging field {} has been DEACTIVATED via environment variable for {}",
                         environmentVariableKey, invokingClass);
            return false;
        }

        LOGGER.debug("Logging field {} is DEACTIVATED by default for {}. Environment variable \"LogRemoteIP = {}\" could not " +
                     "be read. It should be set to true to activate Logging of remote IPs or to false to deactivate it",
                     environmentVariableKey, invokingClass, tempEnvVariable);
        return false;

    }

    public boolean isLogSensitiveConnectionData() {
        return logSensitiveConnectionData;
    }

    public boolean isLogRemoteUserField() {
        return logRemoteUserField;
    }

    public boolean isLogRefererField() {
        return logRefererField;
    }

    public boolean isLogSslHeaders() {
        return logSslHeaders;
    }
}
