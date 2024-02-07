package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

/**
 * <p>
 * The {@link LogContextToRequestAttributeFilter} adds the current {@link MDC}
 * as a request attribute to the current request. This helps with
 * asynchronous request handling, where the MDC is not propagated correctly
 * across thread boundaries. The correct LogContext can be restored by
 * {@code MDC.setContextMap((Map<String, String>) httpRequest.getAttribute(MDC.class.getName()))}.
 * </p>
 * 
 * <p>
 * Adding the {@link MDC} as a request attribute is also done by
 * {@link GenerateRequestLogFilter}. You only need the
 * {@link LogContextToRequestAttributeFilter}, if you do not use
 * {@link GenerateRequestLogFilter}. The default configuration is to generate
 * the request logs. So this filter is not used there.
 * </p>
 */
public class LogContextToRequestAttributeFilter extends AbstractLoggingFilter {

    @Override
    protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute(MDC.class.getName(), MDC.getCopyOfContextMap());
    }

}
