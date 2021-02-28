package com.sap.hcp.cf.logging.jersey.filter;

import java.net.URI;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public class ClientRequestContextAdapter implements RequestContextAdapter {

    public static final String LAYER_NAME = "[JERSEY.CLIENT]";

    private final ClientRequestContext ctx;

    public ClientRequestContextAdapter(ClientRequestContext requestContext) {
        ctx = requestContext;
    }

    @Override
    public String getHeader(String headerName) {
        return ctx.getHeaderString(headerName);
    }

    @Override
    public String getMethod() {
        return ctx.getMethod();
    }

    @Override
    public URI getUri() {
        return ctx.getUri();
    }

    @Override
    public String getName() {
        return LAYER_NAME;
    }

    @Override
    public Direction getDirection() {
        return Direction.OUT;
    }

    @Override
    public void setHeader(String headerName, String headerValue) {
        if (headerName != null && headerValue != null) {
            MultivaluedMap<String, Object> headers = ctx.getHeaders();
            headers.add(headerName, headerValue);
        }
    }

    @Override
    public String getUser() {
        return null;
    }

    @Override
    public long getRequestSize() {
        return -1;
    }

	@Override
	public String getHeader(HttpHeader httpHeader) {
		return getHeader(httpHeader.getName());
	}
}
