package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
public class LogContextToRequestAttributeFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Captor
    private ArgumentCaptor<Map<String, String>> addedAttribute;

    @Test
    public void addsContextMapAsRequestAttribute() throws Exception {
        MDC.clear();
        MDC.put("this key", "this value");
        MDC.put("that key", "that value");

        new LogContextToRequestAttributeFilter().doFilter(request, response, chain);

        verify(request).setAttribute(eq(MDC.class.getName()), addedAttribute.capture());
        assertThat(addedAttribute.getValue().size(), is(2));
        assertThat(addedAttribute.getValue(), both(hasEntry("this key", "this value")).and(hasEntry("that key",
                                                                                                   "that value")));
    }
    
    @Test
    public void addedAttributeCanBeUsedToConfigureMDC() throws Exception {
        MDC.put("this key", "this value");
        MDC.put("that key", "that value");
        new LogContextToRequestAttributeFilter().doFilter(request, response, chain);
        verify(request).setAttribute(eq(MDC.class.getName()), addedAttribute.capture());
        MDC.clear();

        MDC.setContextMap(addedAttribute.getValue());
        
        assertThat(MDC.get("this key"), is(equalTo("this value")));
        assertThat(MDC.get("that key"), is(equalTo("that value")));
    }
}
