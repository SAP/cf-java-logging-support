package com.sap.hcp.cf.logging.common.request;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;

public enum HttpHeaders implements HttpHeader {

	CONTENT_LENGTH("content-length"),
	CONTENT_TYPE("content-type"),
	REFERER("referer"),
	X_FORWARDED_FOR("x-forwarded-for"),
	X_VCAP_REQUEST_ID("x-vcap-request-id"),
	CORRELATION_ID("X-CorrelationID", Fields.CORRELATION_ID, true, X_CORRELATION_ID, X_VCAP_REQUEST_ID),
	X_CORRELATION_ID("X-Correlation-ID"),
	TENANT_ID("tenantid", Fields.TENANT_ID, true);

	private HttpHeaders(String name) {
		this(name, null, false);
	}

	private HttpHeaders(String name, String field, boolean isPropagated, HttpHeaders... aliases) {
		this.name = name;
		this.field = field;
		this.isPropagated = isPropagated;
		this.aliases = unmodifiableList(asList(aliases));
	}

	private String name;
	private String field;
	private boolean isPropagated;
	private List<HttpHeader> aliases;

	@Override
	public boolean isPropagated() {
		return isPropagated;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getField() {
		return field;
	}

	@Override
	public String getFieldValue() {
		return field != null ? LogContext.get(field) : Defaults.UNKNOWN;
	}

	@Override
	public List<HttpHeader> getAliases() {
		return aliases;
	}

	public static List<HttpHeaders> propagated() {
		return LazyPropagatedHeaderHolder.PROPAGATED;
	}

	private static class LazyPropagatedHeaderHolder {
		public static final List<HttpHeaders> PROPAGATED = createPropagated();

		private static List<HttpHeaders> createPropagated() {
			List<HttpHeaders> propagated = new ArrayList<>();
			for (HttpHeaders current : values()) {
				if (current.isPropagated()) {
					propagated.add(current);
				}
			}
			return Collections.unmodifiableList(propagated);
		}
	}
}
