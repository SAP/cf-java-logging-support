package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.request.RequestRecord;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@RunWith(MockitoJUnitRunner.class)
public class GenerateRequestLogFilterTest {

    @Rule
    public SystemOutRule systemOut = new SystemOutRule();

    @Mock
    private RequestRecordFactory requestRecordFactory;

    private RequestRecord requestRecord = new RequestRecord("TEST");;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Captor
    private ArgumentCaptor<HttpServletRequest> forwardedRequest;
    @Captor
    private ArgumentCaptor<HttpServletResponse> forwardedResponse;


    @Before
    public void setUp() throws Exception {
        MDC.clear();
        when(requestRecordFactory.create(any())).thenReturn(requestRecord);
        doNothing().when(chain).doFilter(forwardedRequest.capture(), forwardedResponse.capture());
    }

    @Test
    @Ignore //TODO: check why this does not work anymore
    public void setsRequestAttribute() throws Exception {
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);
        verify(request).setAttribute(eq(MDC.class.getName()), anyMap());
    }

    @Test
    public void wrapsRequest() throws Exception {
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        assertThat(forwardedRequest.getValue(), is(instanceOf(LoggingContextRequestWrapper.class)));
        LoggingContextRequestWrapper wrappedRequest = (LoggingContextRequestWrapper) forwardedRequest.getValue();
        assertThat(wrappedRequest.getRequest(), is(instanceOf(ContentLengthTrackingRequestWrapper.class)));
    }

    @Test
    public void doesNotCreateContentLengthTrackingRequestWrapperIfDisabled() throws Exception {
        GenerateRequestLogFilter filter = new GenerateRequestLogFilter(requestRecordFactory);
        filter.init(when(mock(FilterConfig.class).getInitParameter("wrapRequest")).thenReturn("false").getMock());

        filter.doFilter(request, response, chain);

        assertThat(forwardedRequest.getValue(), is(instanceOf(LoggingContextRequestWrapper.class)));
        LoggingContextRequestWrapper wrappedRequest = (LoggingContextRequestWrapper) forwardedRequest.getValue();
        assertThat(wrappedRequest.getRequest(), is(not(instanceOf(ContentLengthTrackingRequestWrapper.class))));
    }

    @Test
    public void wrapsResponse() throws Exception {
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        assertThat(forwardedResponse.getValue(), is(instanceOf(ContentLengthTrackingResponseWrapper.class)));
    }

    @Test
    public void doesNotCreateContentLengthTrackingResponseWrapperIfDisabled() throws Exception {
        GenerateRequestLogFilter filter = new GenerateRequestLogFilter(requestRecordFactory);
        filter.init(when(mock(FilterConfig.class).getInitParameter("wrapResponse")).thenReturn("false").getMock());

        filter.doFilter(request, response, chain);

        assertThat(forwardedResponse.getValue(), is(not(instanceOf(ContentLengthTrackingResponseWrapper.class))));
    }

    @Test
    public void doesNotWriteLogOnStartAsync() throws Exception {
        when(request.isAsyncStarted()).thenReturn(true);
        
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        assertThat(systemOut.toString(), isEmptyOrNullString());
    }

    @Test
    public void directlyForwardsRequestResponseWhenLogIsDisabled() throws Exception {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(RequestLogger.class).setLevel(Level.OFF);
        
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(systemOut.toString(), isEmptyOrNullString());

        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(RequestLogger.class).setLevel(Level.INFO);

    }
    
}
