package com.sap.hcp.cf.logging.servlet.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.RequestRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Enumeration;
import java.util.List;

import static java.util.Collections.enumeration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, ConsoleExtension.class })
public class GenerateRequestLogFilterTest {

    private static final FilterConfig NO_REQUEST_WRAPPING = new NoRequestWrappingConfig();
    private final RequestRecord requestRecord = new RequestRecord("TEST");
    @Mock
    private RequestRecordFactory requestRecordFactory;
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

    @BeforeEach
    public void setUp() throws Exception {
        MDC.clear();
        when(requestRecordFactory.create(any())).thenReturn(requestRecord);
        doNothing().when(chain).doFilter(forwardedRequest.capture(), forwardedResponse.capture());
        MDC.put("some key", "some value");
    }

    @Test
    public void setsRequestAttribute() throws Exception {
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);
        verify(request).setAttribute(eq(MDC.class.getName()), anyMap());
    }

    @Test
    public void wrapsRequest() throws Exception {
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        assertThat(forwardedRequest.getValue()).isInstanceOf(LoggingContextRequestWrapper.class);
        LoggingContextRequestWrapper wrappedRequest = (LoggingContextRequestWrapper) forwardedRequest.getValue();
        assertThat(wrappedRequest.getRequest()).isInstanceOf(ContentLengthTrackingRequestWrapper.class);
    }

    @Test
    public void doesNotCreateContentLengthTrackingRequestWrapperIfDisabled() throws Exception {
        GenerateRequestLogFilter filter = new GenerateRequestLogFilter(requestRecordFactory);
        filter.init(NO_REQUEST_WRAPPING);

        filter.doFilter(request, response, chain);

        assertThat(forwardedRequest.getValue()).isInstanceOf(LoggingContextRequestWrapper.class);
        LoggingContextRequestWrapper wrappedRequest = (LoggingContextRequestWrapper) forwardedRequest.getValue();
        assertThat(wrappedRequest.getRequest()).isNotInstanceOf(ContentLengthTrackingRequestWrapper.class);
    }

    @Test
    public void wrapsResponse() throws Exception {
        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        assertThat(forwardedRequest.getValue()).isInstanceOf(LoggingContextRequestWrapper.class);
    }

    @Test
    public void doesNotCreateContentLengthTrackingResponseWrapperIfDisabled() throws Exception {
        GenerateRequestLogFilter filter = new GenerateRequestLogFilter(requestRecordFactory);
        filter.init(NO_REQUEST_WRAPPING);

        filter.doFilter(request, response, chain);

        assertThat(forwardedResponse.getValue()).isNotInstanceOf(ContentLengthTrackingResponseWrapper.class);
    }

    @Test
    public void doesNotWriteLogOnStartAsync(ConsoleOutput console) throws Exception {
        when(request.isAsyncStarted()).thenReturn(true);

        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        assertThat(console.getAllEvents()).isEmpty();
    }

    @Test
    public void directlyForwardsRequestResponseWhenLogIsDisabled(ConsoleOutput console) throws Exception {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(RequestLogger.class).setLevel(Level.OFF);
        Mockito.reset(requestRecordFactory);

        new GenerateRequestLogFilter(requestRecordFactory).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(console.getAllEvents()).isEmpty();

        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(RequestLogger.class).setLevel(Level.INFO);

    }

    private static class NoRequestWrappingConfig implements FilterConfig {
        @Override
        public String getFilterName() {
            return "no-request-wrapping";
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String s) {
            return "false";
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return enumeration(List.of("wrapRequest"));
        }
    }
}
