package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.Filter;
import org.slf4j.MDC;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * <p>
 * The {@link RequestLoggingFilter} extracts information from HTTP requests and can create request logs. It will read
 * several HTTP Headers and store them in the SLF4J MDC, so that all log messages created during request handling will
 * have those additional fields. It will also instrument the request to generate a request log containing metrics such
 * as request and response sizes and response time. This instrumentation can be disabled by denying logs from
 * {@link RequestLogger} with marker "request".
 * </p>
 * <p>
 * This filter will generate a correlation id, from the HTTP header "X-CorrelationID" falling back to
 * "x-vcap-request-id" if not found or using a random UUID. The correlation id will be added as an HTTP header
 * "X-CorrelationID" to the response if possible.
 * </p>
 * <p>
 * This filter supports dynamic log levels activated by JWT tokens in HTTP headers. Setup and processing of these tokens
 * can be changed with own implementations of {@link DynamicLogLevelFilter}.
 * </p>
 * <p>
 * To use the filter, it needs to be added to the servlet configuration. It has a default constructor to support web.xml
 * configuration. You can customize the filter by creating your own subclass of {@link CompositeFilter} and mix and
 * match any of the provided filters and add your own implementation:
 * </p>
 * <ul>
 * <li>{@link AddVcapEnvironmentToLogContextFilter} provide application metadata
 * (app_id, app_name, ...) from environment</li>
 * <li>{@link AddHttpHeadersToLogContextFilter} provides certain HTTP headers to
 * {@link MDC}</li>
 * <li>{@link CorrelationIdFilter} extracts "X-CorrelationId" HTTP header or
 * generates new to add to {@link MDC}</li>
 * <li>{@link DynamicLogLevelFilter} supports JWT based dynamic changes of log
 * level per request</li>
 * <li>{@link GenerateRequestLogFilter} instruments the request to generate the
 * final request log</li>
 * </ul>
 */
public class RequestLoggingFilter extends CompositeFilter {

    /**
     * This is no longer used.
     */
    @Deprecated
    public static final String LOG_PROVIDER = "[SERVLET]";

    /**
     * Please use {@link GenerateRequestLogFilter#WRAP_RESPONSE_INIT_PARAM}.
     */
    @Deprecated
    public static final String WRAP_RESPONSE_INIT_PARAM = "wrapResponse";

    /**
     * Please use {@link GenerateRequestLogFilter#WRAP_REQUEST_INIT_PARAM}.
     */
    @Deprecated
    public static final String WRAP_REQUEST_INIT_PARAM = "wrapRequest";

    public RequestLoggingFilter() {
        super(getDefaultFilters());
    }

    public RequestLoggingFilter(RequestRecordFactory requestRecordFactory) {
        super(Stream.of(getDefaultFilters()).map(injectFactory(requestRecordFactory)).toArray(Filter[]::new));
    }

    public static Filter[] getDefaultFilters() {
        return new Filter[] { new AddVcapEnvironmentToLogContextFilter(), new AddHttpHeadersToLogContextFilter(),
                              new CorrelationIdFilter(), new DynamicLogLevelFilter(), new GenerateRequestLogFilter() };
    }

    private static UnaryOperator<Filter> injectFactory(RequestRecordFactory requestRecordFactory) {
        return filter -> filter instanceof GenerateRequestLogFilter
                ? new GenerateRequestLogFilter(requestRecordFactory)
                : filter;
    }
}
