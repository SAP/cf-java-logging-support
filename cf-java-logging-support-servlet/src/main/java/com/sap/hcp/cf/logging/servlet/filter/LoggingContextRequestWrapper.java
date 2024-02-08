package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class LoggingContextRequestWrapper extends HttpServletRequestWrapper {

    private RequestLogger loggingVisitor;

    public LoggingContextRequestWrapper(HttpServletRequest request, RequestLogger loggingVisitor) {
        super(request);
        this.loggingVisitor = loggingVisitor;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return new LoggingAsyncContextImpl(super.startAsync(), loggingVisitor);
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        return new LoggingAsyncContextImpl(super.startAsync(servletRequest, servletResponse), loggingVisitor);
    }
}
