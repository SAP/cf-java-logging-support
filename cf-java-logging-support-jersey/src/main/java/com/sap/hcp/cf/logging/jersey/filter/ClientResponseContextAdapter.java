package com.sap.hcp.cf.logging.jersey.filter;

import javax.ws.rs.client.ClientResponseContext;

import com.sap.hcp.cf.logging.common.request.HttpHeader;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public class ClientResponseContextAdapter implements ResponseContextAdapter {

	private final ClientResponseContext ctx;

	public ClientResponseContextAdapter(ClientResponseContext responseContext) {
		ctx = responseContext;
	}

	@Override
	public String getHeader(HttpHeader header) {
		String headerName = header.getName();
		return ctx.getHeaderString(headerName);
	}

	@Override
	public long getStatus() {
		return ctx.getStatus();
	}

	@Override
	public long getLength() {
		return ctx.getLength();
	}

}
