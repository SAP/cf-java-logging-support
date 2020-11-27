package com.sap.hcp.cf.logging.servlet.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.servlet.dynlog.DynLogConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.DynLogEnvironment;
import com.sap.hcp.cf.logging.servlet.dynlog.DynamicLogLevelProcessor;

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
 * This filter supports dynamic log levels activated by JWT tokens in HTTP
 * headers. Setup and processing of these tokens can be changed with own
 * implementations of {@link DynLogEnvironment} and
 * {@link DynamicLogLevelProcessor}. For integration provide a subclass of
 * {@link RequestLoggingFilter} and overwrite
 * {@link RequestLoggingFilter#getDynLogConfiguration()} and
 * {@link RequestLoggingFilter#getDynLogLevelProcessor()}.
 * </p>
 * <p>
 * To use the filter, it needs to be added to the servlet configuration. It has
 * a default constructor to support web.xml configuration. There are several
 * other constructors that support some customization, in case dynamic
 * configuration (e.g. Spring Boot) is used. You can add further customization
 * by subclassing the filter and overwrite its methods.
 * </p>
 */
public class RequestLoggingFilter extends RequestLoggingBaseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    public static final String LOG_PROVIDER = RequestLoggingBaseFilter.LOG_PROVIDER;
    public static final String WRAP_RESPONSE_INIT_PARAM = RequestLoggingBaseFilter.WRAP_REQUEST_INIT_PARAM;
    public static final String WRAP_REQUEST_INIT_PARAM = RequestLoggingBaseFilter.WRAP_REQUEST_INIT_PARAM;

    private ConcurrentInitializer<DynLogConfiguration> dynLogEnvironment;
    private ConcurrentInitializer<DynamicLogLevelProcessor> dynamicLogLevelProcessor;

    public RequestLoggingFilter() {
        this(createDefaultRequestRecordFactory());
    }

    public RequestLoggingFilter(RequestRecordFactory requestRecordFactory) {
        this(requestRecordFactory, createDefaultDynLogEnvironment());
    }

    private static ConcurrentInitializer<DynLogConfiguration> createDefaultDynLogEnvironment() {
        DynLogEnvironment environment = new DynLogEnvironment();
        return () -> environment;
    }

    public RequestLoggingFilter(ConcurrentInitializer<DynLogConfiguration> dynLogEnvironment) {
        this(createDefaultRequestRecordFactory(), dynLogEnvironment);
    }
    
    public RequestLoggingFilter(RequestRecordFactory requestRecordFactory,
                                ConcurrentInitializer<DynLogConfiguration> dynLogEnvironment) {
        super(requestRecordFactory);
        this.dynLogEnvironment = dynLogEnvironment;
        this.dynamicLogLevelProcessor = new LazyInitializer<DynamicLogLevelProcessor>() {

            @Override
            protected DynamicLogLevelProcessor initialize() throws ConcurrentException {
                return getDynLogConfiguration().map(DynLogConfiguration::getRsaPublicKey).map(DynamicLogLevelProcessor::new)
                                             .get();
            }
        };
    }

    public RequestLoggingFilter(ConcurrentInitializer<DynLogConfiguration> dynLogEnvironment,
                                ConcurrentInitializer<DynamicLogLevelProcessor> dynamicLogLevelProcessor) {
        this(createDefaultRequestRecordFactory(), dynLogEnvironment, dynamicLogLevelProcessor);
    }

    public RequestLoggingFilter(RequestRecordFactory requestRecordFactory,
                                ConcurrentInitializer<DynLogConfiguration> dynLogEnvironment,
                                ConcurrentInitializer<DynamicLogLevelProcessor> dynamicLogLevelProcessor) {
        super(requestRecordFactory);
        this.dynLogEnvironment = dynLogEnvironment;
        this.dynamicLogLevelProcessor = dynamicLogLevelProcessor;
    }

    protected Optional<DynLogConfiguration> getDynLogConfiguration() {
        try {
            return Optional.of(dynLogEnvironment.get());
        } catch (ConcurrentException cause) {
            LOG.debug("Cannot initialize dynamic log level environment. Will continue without this feature", cause);
            return Optional.empty();
        }
    }

    protected Optional<DynamicLogLevelProcessor> getDynLogLevelProcessor() {
        try {
            if (getDynLogConfiguration().map(DynLogConfiguration::getRsaPublicKey).isPresent()) {
                return Optional.of(dynamicLogLevelProcessor.get());
            }
        } catch (ConcurrentException cause) {
            LOG.debug("Cannot initialize dynamic log level processor. Will continue without this feature", cause);
        }
        return Optional.empty();
    }

    @Override
    protected void doFilterRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain)
                                                                                                                        throws IOException,
                                                                                                                        ServletException {
        activateDynamicLogLevels(httpRequest);
        try {
            super.doFilterRequest(httpRequest, httpResponse, chain);
        } finally {
            deactivateDynamicLogLevels();
        }
    }

    private void activateDynamicLogLevels(HttpServletRequest httpRequest) {
        getDynLogLevelProcessor().ifPresent(processor -> {
            getDynLogConfiguration().map(env -> env.getDynLogHeaderValue(httpRequest)).ifPresent(
                                                                                               processor::copyDynamicLogLevelToMDC);
        });
    }

    private void deactivateDynamicLogLevels() {
        getDynLogLevelProcessor().ifPresent(DynamicLogLevelProcessor::removeDynamicLogLevelFromMDC);
    }

}
