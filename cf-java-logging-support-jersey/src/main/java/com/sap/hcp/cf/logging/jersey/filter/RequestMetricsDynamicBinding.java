package com.sap.hcp.cf.logging.jersey.filter;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
@Provider
public class RequestMetricsDynamicBinding implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        context.register(RequestMetricsContainerRequestFilter.class);
        context.register(RequestMetricsContainerResponseFilter.class);
    }

}
