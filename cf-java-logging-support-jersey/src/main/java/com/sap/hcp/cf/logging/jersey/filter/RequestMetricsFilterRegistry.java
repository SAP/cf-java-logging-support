package com.sap.hcp.cf.logging.jersey.filter;

import javax.ws.rs.core.Configurable;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public class RequestMetricsFilterRegistry {

    public static void registerContainerFilters(Configurable<ResourceConfig> config) {
        config.register(RequestMetricsContainerRequestFilter.class);
        config.register(RequestMetricsContainerResponseFilter.class);
    }

    public static void registerClientFilters(ClientConfig clientConfig) {
        clientConfig.register(RequestMetricsClientRequestFilter.class);
        clientConfig.register(RequestMetricsClientResponseFilter.class);
    }
}
