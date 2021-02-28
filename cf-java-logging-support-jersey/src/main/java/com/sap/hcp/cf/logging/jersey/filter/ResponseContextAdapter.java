package com.sap.hcp.cf.logging.jersey.filter;

import com.sap.hcp.cf.logging.common.request.HttpHeader;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public interface ResponseContextAdapter {

	public String getHeader(HttpHeader header);

	public long getStatus();

	public long getLength();

}
