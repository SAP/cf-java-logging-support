package com.sap.hcp.cf.logging.servlet.filter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sap.hcp.cf.logging.servlet.dynlog.DynamicLogLevelConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.DynamicLogLevelProcessor;

@RunWith(MockitoJUnitRunner.class)
public class DynamicLogLevelFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Mock
    private DynamicLogLevelConfiguration configuration;
    @Mock
    private DynamicLogLevelProcessor processor;

    @Test
    public void forwardsHeaderToProcessor() throws Exception {
        when(configuration.getDynLogHeaderValue(request)).thenReturn("header-value");

        new DynamicLogLevelFilter(() -> configuration, () -> processor).doFilter(request, response, chain);

        verify(processor).copyDynamicLogLevelToMDC("header-value");
    }

    @Test
    public void removesDynamicLogLevelFromMDC() throws Exception {
        new DynamicLogLevelFilter(() -> configuration, () -> processor).doFilter(request, response, chain);

        verify(processor).removeDynamicLogLevelFromMDC();
    }

    @Test
    public void doesNotCallProcessorOnMissingHeader() throws Exception {
        new DynamicLogLevelFilter(() -> configuration, () -> processor).doFilter(request, response, chain);

        verify(processor).removeDynamicLogLevelFromMDC();
        verifyNoMoreInteractions(processor);

    }

    @Test
    public void doesNotFailOnDefaultConfiguration() throws Exception {
        new DynamicLogLevelFilter().doFilter(request, response, chain);
    }

    @Test
    public void doesNotFailOnAbsentConfiguration() throws Exception {
        new DynamicLogLevelFilter(() -> null).doFilter(request, response, chain);

    }
}
