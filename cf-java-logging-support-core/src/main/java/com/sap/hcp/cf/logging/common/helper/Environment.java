package com.sap.hcp.cf.logging.common.helper;

public class Environment {

    public static final String LOG_SENSITIVE_CONNECTION_DATA = "LOG_SENSITIVE_CONNECTION_DATA";
    public static final String LOG_REMOTE_USER = "LOG_REMOTE_USER";
    public static final String LOG_REFERER = "LOG_REFERER";
    public static final String LOG_SSL_HEADERS = "LOG_SSL_HEADERS";

    public String getVariable(String name) {
        return System.getenv(name);
    }
}
