package com.sap.hcp.cf.logging.servlet.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.LogOptionalFieldsSettings;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord;

/**
 * <p>
 * A simple servlet filter that logs HTTP request processing info. It will read
 * several HTTP Headers and store them in the SLF4J MDC, so that all log
 * messages created during request handling will have those additional fields.
 * It will also instrument the request to generate a request log containing
 * metrics such as request and response sizes and response time. This
 * instrumentation can be disabled by denying logs from {@link RequestLogger}
 * with marker "request".
 * </p>
 * <p>
 * This filter will generate a correlation id, from the HTTP header
 * "X-CorrelationID" falling back to "x-vcap-request-id" if not found or using a
 * random UUID. The correlation id will be added as an HTTP header
 * "X-CorrelationID" to the response if possible.
 * </p>
 * <p>
 * To use the filter, it needs to be added to the servlet configuration. It has
 * a default constructor to support web.xml configuration. There are several
 * other constructors that support some customization, in case dynamic
 * configuration (e.g. Spring Boot) is used. You can add further customization
 * by subclassing the filter and overwrite its methods.
 * </p>
 */
public class RequestLoggingBaseFilter implements Filter {

    public static final String LOG_PROVIDER = "[SERVLET]";
    public static final String WRAP_RESPONSE_INIT_PARAM = "wrapResponse";
    public static final String WRAP_REQUEST_INIT_PARAM = "wrapRequest";

    private boolean wrapResponse = true;
    private boolean wrapRequest = true;
    private RequestRecordFactory requestRecordFactory;

    public RequestLoggingBaseFilter() {
        this(createDefaultRequestRecordFactory());
    }

    protected static RequestRecordFactory createDefaultRequestRecordFactory() {
        String invokingClass = RequestLoggingBaseFilter.class.getName();
        LogOptionalFieldsSettings logOptionalFieldsSettings = new LogOptionalFieldsSettings(invokingClass);
        return new RequestRecordFactory(logOptionalFieldsSettings);
    }

    public RequestLoggingBaseFilter(RequestRecordFactory requestRecordFactory) {
        this.requestRecordFactory = requestRecordFactory;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String value = filterConfig.getInitParameter(WRAP_RESPONSE_INIT_PARAM);
        if (value != null && "false".equalsIgnoreCase(value)) {
            wrapResponse = false;
        }
        value = filterConfig.getInitParameter(WRAP_REQUEST_INIT_PARAM);
        if (value != null && "false".equalsIgnoreCase(value)) {
            wrapRequest = false;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        if (HttpServletRequest.class.isAssignableFrom(request.getClass()) && HttpServletResponse.class.isAssignableFrom(
                                                                                                                        response.getClass())) {
            doFilterRequest((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        MDC.clear();
    }

    protected void doFilterRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain)
                                                                                                                      throws IOException,
                                                                                                                      ServletException {
        /*
         * -- make sure correlation id is read from headers
         */
        LogContext.initializeContext(HttpHeaderUtilities.getHeaderValue(httpRequest, HttpHeaders.CORRELATION_ID));

        try {

            RequestRecord rr = requestRecordFactory.create(httpRequest);
            httpRequest.setAttribute(MDC.class.getName(), MDC.getCopyOfContextMap());

            if (!httpResponse.isCommitted() && httpResponse.getHeader(HttpHeaders.CORRELATION_ID.getName()) == null) {
                httpResponse.setHeader(HttpHeaders.CORRELATION_ID.getName(), LogContext.getCorrelationId());
            }

            /*
             * If request logging is disabled skip request instrumentation and
             * continue the filter chain immediately.
             */
            if (!RequestLogger.isRequestLoggingEnabled()) {
                doFilter(chain, httpRequest, httpResponse);
                return;
            }

            /*
             * -- we essentially do three things here: -- a) we create a log
             * record using our library and log it via STDOUT -- b) keep track
             * of certain header fields so that they are available in later
             * processing steps -- b) inject a response wrapper to keep track of
             * content length (hopefully)
             */
            if (wrapResponse) {
                httpResponse = new ContentLengthTrackingResponseWrapper(httpResponse);
            }

            if (wrapRequest) {
                httpRequest = new ContentLengthTrackingRequestWrapper(httpRequest);
            }

            RequestLogger loggingVisitor = new RequestLogger(rr, httpRequest, httpResponse);
            httpRequest = new LoggingContextRequestWrapper(httpRequest, loggingVisitor);

            /* -- start measuring right before calling up the filter chain -- */
            rr.start();
            doFilter(chain, httpRequest, httpResponse);

            if (!httpRequest.isAsyncStarted()) {
                loggingVisitor.logRequest();
            }
            /*
             * -- close this
             */
        } finally {
            resetLogContext();
        }
    }

    private void resetLogContext() {
        for (HttpHeader header: HttpHeaders.propagated()) {
            LogContext.remove(header.getField());
        }
        LogContext.resetContextFields();
    }

    private void doFilter(FilterChain chain, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
                                                                                                               throws IOException,
                                                                                                               ServletException {
        if (chain != null) {
            chain.doFilter(httpRequest, httpResponse);
        }
    }

}
