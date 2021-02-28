package com.sap.hcp.cf.logging.jersey.filter;

import java.net.URI;
import java.security.Principal;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public class ContainerRequestContextAdapter implements RequestContextAdapter {

	public static final String LAYER_NAME = "[JERSEY.CONTAINER]";

	private final ContainerRequestContext ctx;

	public ContainerRequestContextAdapter(ContainerRequestContext requestContext) {
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
		return ctx.getUriInfo().getRequestUri();
	}

	@Override
	public String getName() {
		return LAYER_NAME;
	}

	@Override
	public Direction getDirection() {
		return Direction.IN;
	}

	@Override
	public void setHeader(String headerName, String headerValue) {
	}

	@Override
	public String getUser() {
		SecurityContext sc = ctx.getSecurityContext();
		if (sc != null) {
			Principal p = sc.getUserPrincipal();
			if (p != null) {
				return p.getName();
			}
		}
		return null;
	}

	@Override
	public long getRequestSize() {
		return ctx.getLength();
	}

	@Override
	public String getHeader(HttpHeader httpHeader) {
		return getHeader(httpHeader.getName());
	}

}
