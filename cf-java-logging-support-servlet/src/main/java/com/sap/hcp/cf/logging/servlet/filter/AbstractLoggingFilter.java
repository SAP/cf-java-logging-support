package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class AbstractLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            doFilterRequest((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Provides a default implementation for handling servlet requests already
     * cast to {@link HttpServletRequest} and {@link HttpServletResponse}.
     * Custom implementations of {@link AbstractLoggingFilter} should overwrite
     * {@link #beforeFilter(HttpServletRequest, HttpServletResponse)} and/or
     * {@link #cleanup(HttpServletRequest, HttpServletResponse)}.
     * 
     * @param request
     *            cast as {@link HttpServletRequest}
     * @param response
     *            cast as {@link HttpServletResponse}
     * @param chain
     *            the {@link FilterChain} to continue request handling
     * @throws IOException
     * @throws ServletException
     */
    protected void doFilterRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                                                                                                                throws IOException,
                                                                                                                ServletException {
        try {
            beforeFilter(request, response);
            chain.doFilter(request, response);
        } finally {
            cleanup(request, response);

        }
    }

    /**
     * Processes the request/response before it is passed along the filter
     * chain. Even if {@link #beforeFilter} fails, {@link #cleanup} will be
     * executed.
     * 
     * @param request
     * @param response
     */
    protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
    }

    /**
     * Cleanup after the request/response was handled by the filter chain. This
     * is executed even in cases of failures during handling or
     * {@link #beforeFilter}. Use this method to reset or clean-up state, e.g.
     * MDC. Be aware, that there may be partially initalized state from
     * {@link #beforeFilter}.
     * 
     * @param request
     * @param response
     */
    protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
    }

    /**
     * This is an empty implementation for filters, that do not require
     * initialization.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    /**
     * this is an empty implementation for tolters, that do not require clean-up
     * of state.
     */
    @Override
    public void destroy() {
        // nothing to do
    }
}
