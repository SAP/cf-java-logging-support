package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.*;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class LoggingAsyncContextImpl implements AsyncContext {

    private AsyncContext asyncContext;

    public LoggingAsyncContextImpl(AsyncContext asyncContext, final RequestLogger requestLogger) {
        this.asyncContext = asyncContext;
        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                requestLogger.logRequest();
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                requestLogger.logRequest();
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                requestLogger.logRequest();
            }
        });
    }

    private Map<String, String> getContextMap() {
        try {
            @SuppressWarnings("unchecked") Map<String, String> fromRequest =
                    (Map<String, String>) getRequest().getAttribute(MDC.class.getName());
            return fromRequest != null ? fromRequest : Collections.emptyMap();
        } catch (ClassCastException ignored) {
            return Collections.emptyMap();
        }
    }

    @Override
    public void start(Runnable run) {
        try {
            Map<String, String> contextMap = getContextMap();
            asyncContext.start(new Runnable() {

                @Override
                public void run() {
                    Map<String, String> currentContextMap = MDC.getCopyOfContextMap();
                    try {
                        MDC.setContextMap(contextMap);
                        run.run();
                    } finally {
                        if (currentContextMap != null) {
                            MDC.setContextMap(currentContextMap);
                        }
                    }
                }
            });
        } catch (ClassCastException ignored) {
            asyncContext.start(run);
        }
    }

    @Override
    public ServletRequest getRequest() {
        return asyncContext.getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return asyncContext.getResponse();
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return asyncContext.hasOriginalRequestAndResponse();
    }

    @Override
    public void dispatch() {
        asyncContext.dispatch();
    }

    @Override
    public void dispatch(String path) {
        asyncContext.dispatch(path);
    }

    @Override
    public void dispatch(ServletContext context, String path) {
        asyncContext.dispatch(context, path);
    }

    @Override
    public void complete() {
        asyncContext.complete();
    }

    @Override
    public void addListener(AsyncListener listener) {
        asyncContext.addListener(listener);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        asyncContext.addListener(listener, servletRequest, servletResponse);
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
        return asyncContext.createListener(clazz);
    }

    @Override
    public long getTimeout() {
        return asyncContext.getTimeout();
    }

    @Override
    public void setTimeout(long timeout) {
        asyncContext.setTimeout(timeout);
    }

}
