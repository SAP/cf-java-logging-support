package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Predicate;

import static com.sap.hcp.cf.logging.common.customfields.CustomField.customField;
import static com.sap.hcp.cf.logging.common.request.HttpHeaders.W3C_TRACEPARENT;
import static java.util.Optional.ofNullable;

/**
 * The {@link CorrelationIdFilter} extracts a correlation id according to
 * {@link HttpHeaders#CORRELATION_ID}. It will generate a random uuid, if no
 * correlation id is found in the headers. In any case the correlation id is set
 * as a response header, if possible
 */
public class CorrelationIdFilter extends AbstractLoggingFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private HttpHeader correlationHeader;
    private HttpHeader traceparentHeader;

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
        LogContext.add(correlationHeader.getField(), correlationId);
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

    private boolean isBlankOrDefault(String value) {
        return value == null || value.isEmpty() || value.equals(Defaults.UNKNOWN);
    }

    private String getCorrelationIdFromTraceparent(HttpServletRequest request) {
        String traceparent = HttpHeaderUtilities.getHeaderValue(request, traceparentHeader);
        return ofNullable(traceparent).filter(not(this::isBlankOrDefault)).map(this::parseTraceparent).orElse(
                                                                                                                        null);
    }

    private <T> Predicate<T> not(Predicate<T> p) {
        return p.negate();
    }

    private String parseTraceparent(String value) {
        String[] tokens = value.split("-");
        return tokens.length >= 2 ? tokens[1] : null;
    }

    private void addCorrelationIdHeader(HttpServletResponse response, String correlationId) {
        if (!response.isCommitted() && response.getHeader(correlationHeader.getName()) == null) {
            response.setHeader(correlationHeader.getName(), correlationId);
        }
    }

    @Override
    protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
        LogContext.remove(correlationHeader.getField());
    }
}
