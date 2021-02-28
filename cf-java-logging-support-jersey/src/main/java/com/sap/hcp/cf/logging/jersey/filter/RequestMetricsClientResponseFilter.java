package com.sap.hcp.cf.logging.jersey.filter;

import static com.sap.hcp.cf.logging.jersey.filter.Utils.REQ_METRICS_KEY;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.request.RequestRecord;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
@Provider
public class RequestMetricsClientResponseFilter implements ClientResponseFilter {

    private final ResponseHandler handler;

    public RequestMetricsClientResponseFilter() {
        handler = new ResponseHandler();
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        try {
            handler.handle(new ClientResponseContextAdapter(responseContext), (RequestRecord) requestContext
                                                                                                            .getProperty(REQ_METRICS_KEY));
        } catch (Exception ex) {
            LoggerFactory.getLogger(RequestMetricsClientResponseFilter.class).error("Can't handle client response", ex);
        }
    }

}
