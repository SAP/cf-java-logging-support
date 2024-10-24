package com.sap.cloud.cf.monitoring.client.configuration;

public class SystemGetEnvWrapper {

    private SystemGetEnvWrapper() {}

    public static java.util.Map<String,String> getenv() {
        return System.getenv();
    }

}
