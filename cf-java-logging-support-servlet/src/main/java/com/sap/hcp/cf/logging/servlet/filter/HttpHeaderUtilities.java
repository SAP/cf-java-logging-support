package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class HttpHeaderUtilities {

    private HttpHeaderUtilities() {
    }

    public static String getHeaderValue(HttpServletRequest httpRequest, HttpHeader header) {
        return getHeaderValue(httpRequest, header, null);
    }

    public static String getHeaderValue(HttpServletRequest httpRequest, HttpHeader header, String defaultValue) {
        String headerValue = getHeaderValueInternal(httpRequest, header);
        if (headerValue != null) {
            return headerValue;
        }
        for (HttpHeader alias: header.getAliases()) {
            String value = getHeaderValueInternal(httpRequest, alias);
            if (value != null) {
                return value;
            }
        }
        String contextFieldValue = getLogContextFieldValue(header);
        if (contextFieldValue != null) {
            return contextFieldValue;
        }
        return defaultValue;
    }

    private static String getHeaderValueInternal(HttpServletRequest httpRequest, HttpHeader header) {
        String headerName = header.getName();
        return httpRequest.getHeader(headerName);
    }

    private static String getLogContextFieldValue(HttpHeader header) {
        if (header.getField() == null) {
            return null;
        }
        return LogContext.get(header.getField());
    }

    public static String getHeaderValue(HttpServletResponse httpResponse, HttpHeader header) {
        return getHeaderValue(httpResponse, header, null);
    }

    public static String getHeaderValue(HttpServletResponse httpResponse, HttpHeader header, String defaultValue) {
        String headerValue = getHeaderValueInternal(httpResponse, header);
        if (headerValue != null) {
            return headerValue;
        }
        for (HttpHeader alias: header.getAliases()) {
            String value = getHeaderValueInternal(httpResponse, alias);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    private static String getHeaderValueInternal(HttpServletResponse httpResponse, HttpHeader header) {
        if (httpResponse == null) {
            return null;
        }
        String headerName = header.getName();
        return httpResponse.getHeader(headerName);
    }

}
