package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.LogOptionalFieldsSettings;
import com.sap.hcp.cf.logging.common.request.RequestRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * The {@link GenerateRequestLogFilter} writes a log message for each incoming request. The message contains metadata
 * and metrics about request and response. It adds the {@link MDC} as a request attribute and wraps the request to
 * support asynchronous request handling. Additionally request and response are wrapped once more to determine request
 * and response sizes. You can disable this second wrapping by setting the init parameters <i>wrapRequest</i> and
 * <i>wrapResponse</i> to {@code false}.
 */

public class GenerateRequestLogFilter extends AbstractLoggingFilter {

    public static final String WRAP_RESPONSE_INIT_PARAM = "wrapResponse";
    public static final String WRAP_REQUEST_INIT_PARAM = "wrapRequest";

    private final RequestRecordFactory requestRecordFactory;

    private boolean wrapResponse = true;
    private boolean wrapRequest = true;

    public GenerateRequestLogFilter() {
        this(new RequestRecordFactory(new LogOptionalFieldsSettings(GenerateRequestLogFilter.class.getName())));
    }

    public GenerateRequestLogFilter(RequestRecordFactory requestRecordFactory) {
        this.requestRecordFactory = requestRecordFactory;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String value = filterConfig.getInitParameter(WRAP_RESPONSE_INIT_PARAM);
        if ("false".equalsIgnoreCase(value)) {
            wrapResponse = false;
        }
        value = filterConfig.getInitParameter(WRAP_REQUEST_INIT_PARAM);
        if ("false".equalsIgnoreCase(value)) {
            wrapRequest = false;
        }
    }

    @Override
    protected void doFilterRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!RequestLogger.isRequestLoggingEnabled()) {
            doFilter(chain, request, response);
            return;
        }

        RequestRecord record = requestRecordFactory.create(request);
        request.setAttribute(MDC.class.getName(), MDC.getCopyOfContextMap());

        if (wrapRequest) {
            request = new ContentLengthTrackingRequestWrapper(request);
        }
        if (wrapResponse) {
            response = new ContentLengthTrackingResponseWrapper(response);
        }

        RequestLogger logger = new RequestLogger(record, request, response);
        request = new LoggingContextRequestWrapper(request, logger);

        record.start();

        try {
            doFilter(chain, request, response);
        } finally {
            if (!request.isAsyncStarted()) {
                logger.logRequest();
            }

        }
    }

    private void doFilter(FilterChain chain, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (chain != null) {
            chain.doFilter(request, response);
        }
    }

}
