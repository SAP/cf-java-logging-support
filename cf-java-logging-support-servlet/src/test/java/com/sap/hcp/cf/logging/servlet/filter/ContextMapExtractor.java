package com.sap.hcp.cf.logging.servlet.filter;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;

import java.util.Map;

public class ContextMapExtractor implements Answer<Void> {

    private Map<String, String> contextMap;

    public Map<String, String> getContextMap() {
        return contextMap;
    }

    public String getField(String name) {
        return contextMap.get(name);
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
        contextMap = MDC.getCopyOfContextMap();
        return null;
    }
}
