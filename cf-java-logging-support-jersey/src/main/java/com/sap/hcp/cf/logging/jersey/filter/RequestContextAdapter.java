package com.sap.hcp.cf.logging.jersey.filter;

import java.net.URI;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

// Jersey support has been deprecated in version 3.4.0 for removal in later versions.
// Please migrate to cf-java-logging-support-servlet.
@Deprecated
public interface RequestContextAdapter {

    public String getHeader(String headerName);

    public void setHeader(String headerName, String headerValue);

    public String getName();

    public String getMethod();

    public String getUser();

    public URI getUri();

    public Direction getDirection();

    public long getRequestSize();

	public String getHeader(HttpHeader httpHeader);

}
