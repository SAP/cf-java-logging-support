package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LongValue;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;

import static com.sap.hcp.cf.logging.servlet.filter.HttpHeaderUtilities.getHeaderValue;

public class RequestLogger {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLogger.class);

    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;
    private RequestRecord requestRecord;

    public RequestLogger(RequestRecord requestRecord, HttpServletRequest httpRequest,
                         HttpServletResponse httpResponse) {
        this.requestRecord = requestRecord;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    public static final boolean isRequestLoggingEnabled() {
        return LOG.isInfoEnabled(Markers.REQUEST_MARKER);
    }

    public void logRequest() {
        requestRecord.stop();
        addRequestHandlingParameters();
        generateLog();
        requestRecord.resetContext();
    }

    private void addRequestHandlingParameters() {
        requestRecord.addValue(Fields.REQUEST_SIZE_B, new LongValue(httpRequest.getContentLength()));
        LongValue responseSize = getResponseSize(httpResponse);
        if (responseSize != null) {
            requestRecord.addValue(Fields.RESPONSE_SIZE_B, responseSize);
        }
        requestRecord.addTag(Fields.RESPONSE_CONTENT_TYPE, getContentType(httpResponse));
        requestRecord.addValue(Fields.RESPONSE_STATUS, new LongValue(httpResponse.getStatus()));
    }

    private LongValue getResponseSize(HttpServletResponse httpResponse) {
        String headerValue = getHeaderValue(httpResponse, HttpHeaders.CONTENT_LENGTH);
        if (headerValue != null) {
            return new LongValue(Long.valueOf(headerValue));
        }
        if (httpResponse != null && httpResponse instanceof ContentLengthTrackingResponseWrapper) {
            ContentLengthTrackingResponseWrapper wrapper = (ContentLengthTrackingResponseWrapper) httpResponse;
            return new LongValue(wrapper.getContentLength());
        }
        return null;
    }

    private String getContentType(HttpServletResponse httpResponse) {
        return getHeaderValue(httpResponse, HttpHeaders.CONTENT_TYPE, Defaults.UNKNOWN);
    }

    private void generateLog() {
        Map<String, String> contextMap = getContextMap();
        Map<String, String> currentContextMap = MDC.getCopyOfContextMap();
        try {
            MDC.setContextMap(contextMap);
            LOG.info(Markers.REQUEST_MARKER, "", requestRecord);
        } finally {
            if (currentContextMap != null) {
                MDC.setContextMap(currentContextMap);
            }
        }
    }

    private Map<String, String> getContextMap() {
        try {
            @SuppressWarnings("unchecked") Map<String, String> fromRequest =
                    (Map<String, String>) httpRequest.getAttribute(MDC.class.getName());
            return fromRequest != null ? fromRequest : Collections.emptyMap();
        } catch (ClassCastException ignored) {
            return Collections.emptyMap();
        }
    }

}
