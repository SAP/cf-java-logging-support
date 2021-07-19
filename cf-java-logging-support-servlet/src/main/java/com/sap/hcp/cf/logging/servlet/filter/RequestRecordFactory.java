package com.sap.hcp.cf.logging.servlet.filter;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static com.sap.hcp.cf.logging.servlet.filter.HttpHeaderUtilities.getHeaderValue;

import javax.servlet.http.HttpServletRequest;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogOptionalFieldsSettings;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord;
import com.sap.hcp.cf.logging.common.request.RequestRecordBuilder;

public class RequestRecordFactory {

	private final LogOptionalFieldsSettings logOptionalFieldsSettings;

	public RequestRecordFactory(LogOptionalFieldsSettings logOptionalFieldsSettings) {
		this.logOptionalFieldsSettings = logOptionalFieldsSettings;
	}

	public RequestRecord create(HttpServletRequest request) {
		boolean isSensitiveConnectionData = logOptionalFieldsSettings.isLogSensitiveConnectionData();
		boolean isLogRemoteUserField = logOptionalFieldsSettings.isLogRemoteUserField();
		boolean isLogRefererField = logOptionalFieldsSettings.isLogRefererField();
		RequestRecordBuilder rrb =  requestRecord("[SERVLET]").addTag(Fields.REQUEST, getFullRequestUri(request))
				.addTag(Fields.METHOD, request.getMethod())
				.addTag(Fields.PROTOCOL, getValue(request.getProtocol()))
				.addOptionalTag(isSensitiveConnectionData, Fields.REMOTE_IP, getValue(request.getRemoteAddr()))
				.addOptionalTag(isSensitiveConnectionData, Fields.REMOTE_HOST, getValue(request.getRemoteHost()))
				.addOptionalTag(isSensitiveConnectionData, Fields.REMOTE_PORT,
						Integer.toString(request.getRemotePort()))
				.addOptionalTag(isSensitiveConnectionData, Fields.X_FORWARDED_FOR,
						getHeader(request, HttpHeaders.X_FORWARDED_FOR))
				.addOptionalTag(isLogRemoteUserField, Fields.REMOTE_USER, getValue(request.getRemoteUser()))
				.addOptionalTag(isLogRefererField, Fields.REFERER, getHeader(request, HttpHeaders.REFERER));
		return rrb.build();
	}

	private String getFullRequestUri(HttpServletRequest request) {
		String queryString = request.getQueryString();
		String requestURI = request.getRequestURI();
		return queryString != null ? requestURI + "?" + queryString : requestURI;
	}

	private String getHeader(HttpServletRequest request, HttpHeader header) {
		return getHeaderValue(request, header, Defaults.UNKNOWN);
	}

	private String getValue(String value) {
		return value != null ? value : Defaults.UNKNOWN;
	}

}
