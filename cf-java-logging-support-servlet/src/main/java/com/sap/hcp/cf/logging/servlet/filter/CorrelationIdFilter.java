package com.sap.hcp.cf.logging.servlet.filter;

import static com.sap.hcp.cf.logging.common.customfields.CustomField.customField;
import static com.sap.hcp.cf.logging.common.request.HttpHeaders.W3C_TRACEPARENT;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.hcp.cf.logging.common.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

/**
 * The {@link CorrelationIdFilter} extracts a correlation id according to
 * {@link HttpHeaders#CORRELATION_ID}. It will generate a random uuid, if no
 * correlation id is found in the headers. In any case the correlation id is set
 * as a response header, if possible
 */
public class CorrelationIdFilter extends AbstractLoggingFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private final HttpHeader correlationHeader;
    private final HttpHeader traceparentHeader;

    public CorrelationIdFilter() {
        this(HttpHeaders.CORRELATION_ID);
    }

    public CorrelationIdFilter(HttpHeader correlationHeader) {
        this(correlationHeader, W3C_TRACEPARENT);
    }

    public CorrelationIdFilter(HttpHeader correlationHeader, HttpHeader traceparentHeader) {
        this.correlationHeader = correlationHeader;
        this.traceparentHeader = traceparentHeader;
    }

    @Override
    protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
        String correlationId = determineCorrelationId(request);
        LogContext.add(Fields.CORRELATION_ID, correlationId);
        addCorrelationIdHeader(response, correlationId);
    }

    private String determineCorrelationId(HttpServletRequest request) {
        String correlationId = HttpHeaderUtilities.getHeaderValue(request, correlationHeader);
        if (isBlankOrDefault(correlationId)) {
            correlationId = getCorrelationIdFromTraceparent(request);
        }
        if (isBlankOrDefault(correlationId)) {
            correlationId = String.valueOf(UUID.randomUUID());
            // add correlation-id as custom field, since it is added to MDC only
            // in the next step
            LOG.debug("Generated new correlation-id <{}>", correlationId, customField(correlationHeader.getField(),
                                                                                      correlationId));
        }
        return correlationId;
    }

    private static boolean isBlankOrDefault(String value) {
        return value == null || value.isEmpty() || value.equals(Defaults.UNKNOWN);
    }

    private String getCorrelationIdFromTraceparent(HttpServletRequest request) {
        String traceparent = HttpHeaderUtilities.getHeaderValue(request, traceparentHeader);
        return isBlankOrDefault(traceparent) ? null : parseTraceparent(traceparent);
    }

    private static String parseTraceparent(String value) {
        int idx1 = value.indexOf('-');
        if (idx1 != -1) {
            ++idx1;
            int idx2 = value.indexOf('-', idx1);
            if (idx2 != -1) {
                // return string between first and second '-'.
                return value.substring(idx1, idx2);
            }
        }
        return null;
    }

    private void addCorrelationIdHeader(HttpServletResponse response, String correlationId) {
        if (!response.isCommitted() && response.getHeader(correlationHeader.getName()) == null) {
            response.setHeader(correlationHeader.getName(), correlationId);
        }
    }

    @Override
    protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
        LogContext.remove(Fields.CORRELATION_ID);
    }
}
