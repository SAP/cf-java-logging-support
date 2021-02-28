package com.sap.hcp.cf.logging.jersey.filter;

import javax.ws.rs.client.Invocation;

import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public class ClientRequestUtils {

	public static Invocation.Builder propagate(Invocation.Builder builder, javax.ws.rs.core.HttpHeaders reqHeaders) {
		if (LogContext.getCorrelationId() == null) {
			LogContext.initializeContext(
					reqHeaders != null ? reqHeaders.getHeaderString(HttpHeaders.CORRELATION_ID.getName()) : null);
		}
		for (HttpHeader header : HttpHeaders.propagated()) {
			builder.header(header.getName(), header.getFieldValue());
		}
		return builder;
	}

}
