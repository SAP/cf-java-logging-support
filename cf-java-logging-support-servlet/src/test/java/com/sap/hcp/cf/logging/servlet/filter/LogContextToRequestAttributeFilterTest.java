package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;

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

        verify(request).setAttribute(matches(MDC.class.getName()), addedAttribute.capture());
        assertThat(addedAttribute.getValue()).hasSize(2) //
                                             .containsEntry("this key", "this value")
                                             .containsEntry("that key", "that value");
    }

    @Test
    public void addedAttributeCanBeUsedToConfigureMDC() throws Exception {
        MDC.put("this key", "this value");
        MDC.put("that key", "that value");
        new LogContextToRequestAttributeFilter().doFilter(request, response, chain);
        verify(request).setAttribute(matches(MDC.class.getName()), addedAttribute.capture());
        MDC.clear();

        MDC.setContextMap(addedAttribute.getValue());

        assertThat(MDC.get("this key")).isEqualTo("this value");
        assertThat(MDC.get("that key")).isEqualTo("that value");
    }
}
