package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogOptionalFieldsSettings;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

@ExtendWith(MockitoExtension.class)
public class RequestLoggingFilterTest {

    private static final String REQUEST_ID = "1234-56-7890-xxx";
    private static final String CORRELATION_ID = "xxx-56-7890-xxx";
    private static final String TENANT_ID = "tenant1";
    private static final String W3C_TRACEPARENT = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    private static final String REQUEST = "/foobar";
    private static final String QUERY_STRING = "baz=bla";
    private static final String FULL_REQUEST = REQUEST + "?" + QUERY_STRING;
    private static final String REMOTE_HOST = "acme.org";
    private static final String REFERER = "my.fancy.com";

    private HttpServletRequest mockReq = mock(HttpServletRequest.class);
    private HttpServletResponse mockResp = mock(HttpServletResponse.class);
    private PrintWriter mockWriter = mock(PrintWriter.class);

    @Mock
    private Appender mockedAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

    @BeforeEach
    public void initMocks() throws IOException {
        Mockito.reset(mockReq, mockResp, mockWriter);
        when(mockResp.getWriter()).thenReturn(mockWriter);

        Map<String, String> contextMap = new HashMap<>();
        Mockito.doAnswer(new Answer<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                contextMap.clear();
                contextMap.putAll((Map<? extends String, ? extends String>) arguments[1]);
                return null;
            }
        }).when(mockReq).setAttribute(eq(MDC.class.getName()), anyMap());//String.class, String.class

        when(mockReq.getAttribute(MDC.class.getName())).thenReturn(contextMap);

        Logger loggerOfInterest = (Logger) LoggerFactory.getLogger(RequestLogger.class.getName());
        loggerOfInterest.addAppender(mockedAppender);
        loggerOfInterest.setLevel(Level.INFO);
    }

    @Test
    public void testSimple() throws IOException, ServletException {
        FilterChain mockFilterChain = mock(FilterChain.class);

        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.REQUEST, lastLine), is("-"));
        //TODO: check assertThat(getField(Fields.CORRELATION_ID, lastLine), not(isEmptyOrNullString()));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), not(isEmptyOrNullString()));
        assertThat(getField(Fields.REQUEST_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REMOTE_HOST, lastLine), is("-"));
        assertThat(getField(Fields.COMPONENT_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.CONTAINER_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REQUEST_SIZE_B, lastLine), is("-1"));
    }

    @Test
    public void testInputStream() throws IOException, ServletException {
        ServletInputStream mockStream = mock(ServletInputStream.class);

        when(mockReq.getInputStream()).thenReturn(mockStream);
        when(mockStream.read()).thenReturn(1);
        FilterChain mockFilterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException,
                                                                                   ServletException {
                request.getInputStream().read();
            }
        };
        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.REQUEST, lastLine), is("-"));
        //TODO: check this.. assertThat(getField(Fields.CORRELATION_ID, lastLine), not(isEmptyOrNullString()));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), not(isEmptyOrNullString()));
        assertThat(getField(Fields.REQUEST_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REMOTE_HOST, lastLine), is("-"));
        assertThat(getField(Fields.COMPONENT_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.CONTAINER_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REQUEST_SIZE_B, lastLine), is("1"));
    }

    @Test
    public void testReader() throws IOException, ServletException {
        BufferedReader reader = new BufferedReader(new StringReader("TEST"));

        when(mockReq.getReader()).thenReturn(reader);
        FilterChain mockFilterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException,
                                                                                   ServletException {
                request.getReader().read();
            }
        };
        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.REQUEST, lastLine), is("-"));
        //TODO: check this.. assertThat(getField(Fields.CORRELATION_ID, lastLine), not(isEmptyOrNullString()));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), not(isEmptyOrNullString()));
        assertThat(getField(Fields.REQUEST_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REMOTE_HOST, lastLine), is("-"));
        assertThat(getField(Fields.COMPONENT_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.CONTAINER_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REQUEST_SIZE_B, lastLine), is("4"));
        assertThat(getField(Fields.TENANT_ID, lastLine), is(nullValue()));
    }

    @Test
    public void testWithActivatedOptionalFields() throws IOException, ServletException {
        when(mockReq.getRequestURI()).thenReturn(REQUEST);
        when(mockReq.getQueryString()).thenReturn(QUERY_STRING);
        when(mockReq.getRemoteHost()).thenReturn(REMOTE_HOST);
        // will also set correlation id
        mockGetHeader(HttpHeaders.X_VCAP_REQUEST_ID, REQUEST_ID);
        mockGetHeader(HttpHeaders.REFERER, REFERER);
        FilterChain mockFilterChain = mock(FilterChain.class);
        LogOptionalFieldsSettings mockOptionalFieldsSettings = mock(LogOptionalFieldsSettings.class);
        when(mockOptionalFieldsSettings.isLogSensitiveConnectionData()).thenReturn(true);
        when(mockOptionalFieldsSettings.isLogRemoteUserField()).thenReturn(true);
        when(mockOptionalFieldsSettings.isLogRefererField()).thenReturn(true);
        RequestRecordFactory requestRecordFactory = new RequestRecordFactory(mockOptionalFieldsSettings);
        Filter requestLoggingFilter = new RequestLoggingFilter(requestRecordFactory);
        requestLoggingFilter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.REQUEST, lastLine), is(FULL_REQUEST));
        //TODO check assertThat(getField(Fields.CORRELATION_ID, lastLine), is(REQUEST_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), is(REQUEST_ID));
        //TODO check assertThat(getField(Fields.REQUEST_ID, lastLine), is(REQUEST_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.REQUEST_ID).toString(), is(REQUEST_ID));
        assertThat(getField(Fields.REMOTE_HOST, lastLine), is(REMOTE_HOST));
        assertThat(getField(Fields.COMPONENT_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.CONTAINER_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.REFERER, lastLine), is(REFERER));
        assertThat(getField(Fields.TENANT_ID, lastLine), is(nullValue()));
    }

    private void mockGetHeader(HttpHeader header, String value) {
        when(mockReq.getHeader(header.getName())).thenReturn(value);
    }

    @Test
    public void testWithSuppressedOptionalFields() throws IOException, ServletException {
        when(mockReq.getRequestURI()).thenReturn(REQUEST);
        when(mockReq.getQueryString()).thenReturn(QUERY_STRING);
        when(mockReq.getRemoteHost()).thenReturn(REMOTE_HOST);
        // will also set correlation id
        mockGetHeader(HttpHeaders.X_VCAP_REQUEST_ID, REQUEST_ID);
        mockGetHeader(HttpHeaders.REFERER, REFERER);
        FilterChain mockFilterChain = mock(FilterChain.class);
        LogOptionalFieldsSettings mockLogOptionalFieldsSettings = mock(LogOptionalFieldsSettings.class);
        when(mockLogOptionalFieldsSettings.isLogSensitiveConnectionData()).thenReturn(false);
        when(mockLogOptionalFieldsSettings.isLogRemoteUserField()).thenReturn(false);
        when(mockLogOptionalFieldsSettings.isLogRefererField()).thenReturn(false);
        RequestRecordFactory requestRecordFactory = new RequestRecordFactory(mockLogOptionalFieldsSettings);
        Filter requestLoggingFilter = new RequestLoggingFilter(requestRecordFactory);
        requestLoggingFilter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.REQUEST, lastLine), is(FULL_REQUEST));
        //TODO: check assertThat(getField(Fields.CORRELATION_ID, lastLine), is(REQUEST_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), is(REQUEST_ID));
        //TODO: check assertThat(getField(Fields.REQUEST_ID, lastLine), is(REQUEST_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.REQUEST_ID).toString(), is(REQUEST_ID));
        assertThat(getField(Fields.REMOTE_IP, lastLine), is("-"));
        assertThat(getField(Fields.REMOTE_HOST, lastLine), is(Defaults.REDACTED));
        assertThat(getField(Fields.COMPONENT_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.CONTAINER_ID, lastLine), is(nullValue()));
        assertThat(getField(Fields.TENANT_ID, lastLine), is(nullValue()));
    }

    @Test
    public void testExplicitCorrelationId() throws IOException, ServletException {
        mockGetHeader(HttpHeaders.CORRELATION_ID, CORRELATION_ID);
        mockGetHeader(HttpHeaders.X_VCAP_REQUEST_ID, REQUEST_ID);
        FilterChain mockFilterChain = mock(FilterChain.class);
        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        //TODO: check this assertThat(getField(Fields.CORRELATION_ID, lastLine), is(CORRELATION_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), is(CORRELATION_ID));
        //TODO: check assertThat(getField(Fields.CORRELATION_ID, lastLine), not(REQUEST_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID).toString(), not(REQUEST_ID));
        //TODO: check assertThat(getField(Fields.REQUEST_ID, lastLine), is(REQUEST_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.REQUEST_ID).toString(), is(REQUEST_ID));
        assertThat(getField(Fields.TENANT_ID, lastLine), is(nullValue()));
    }

    @Test
    public void testExplicitW3cTraceparent() throws IOException, ServletException {
        mockGetHeader(HttpHeaders.W3C_TRACEPARENT, W3C_TRACEPARENT);
        FilterChain mockFilterChain = mock(FilterChain.class);
        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        //TODO: check assertThat(getField(Fields.W3C_TRACEPARENT, lastLine), is(W3C_TRACEPARENT));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.W3C_TRACEPARENT).toString(), is(W3C_TRACEPARENT));
    }

    @Test
    public void testExplicitTenantId() throws IOException, ServletException {
        mockGetHeader(HttpHeaders.TENANT_ID, TENANT_ID);
        mockGetHeader(HttpHeaders.X_VCAP_REQUEST_ID, REQUEST_ID);
        FilterChain mockFilterChain = mock(FilterChain.class);
        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        //TODO: check assertThat(getField(Fields.TENANT_ID, lastLine), is(TENANT_ID));
        assertThat(loggingEvent.getMDCPropertyMap().get(Fields.TENANT_ID).toString(), is(TENANT_ID));

    }

    @Test
    public void testProxyHeadersLogged() throws Exception {
        LogOptionalFieldsSettings settings = mock(LogOptionalFieldsSettings.class);
        when(settings.isLogSensitiveConnectionData()).thenReturn(true).getMock();
        mockGetHeader(HttpHeaders.X_CUSTOM_HOST, "custom.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_FOR, "1.2.3.4,5.6.7.8");
        mockGetHeader(HttpHeaders.X_FORWARDED_HOST, "requested.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_PROTO, "https");
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(new RequestRecordFactory(settings));
        filter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.X_CUSTOM_HOST, lastLine), is("custom.example.com"));
        assertThat(getField(Fields.X_FORWARDED_FOR, lastLine), is("1.2.3.4,5.6.7.8"));
        assertThat(getField(Fields.X_FORWARDED_HOST, lastLine), is("requested.example.com"));
        assertThat(getField(Fields.X_FORWARDED_PROTO, lastLine), is("https"));
    }

    @Test
    public void testProxyHeadersRedactedByDefault() throws Exception {
        mockGetHeader(HttpHeaders.X_CUSTOM_HOST, "custom.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_FOR, "1.2.3.4,5.6.7.8");
        mockGetHeader(HttpHeaders.X_FORWARDED_HOST, "requested.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_PROTO, "https");
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter();
        filter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.X_CUSTOM_HOST, lastLine), is("redacted"));
        assertThat(getField(Fields.X_FORWARDED_FOR, lastLine), is("redacted"));
        assertThat(getField(Fields.X_FORWARDED_HOST, lastLine), is("redacted"));
        assertThat(getField(Fields.X_FORWARDED_PROTO, lastLine), is("redacted"));
    }

    @Test
    public void testSslHeadersLogged() throws Exception {
        LogOptionalFieldsSettings settings = mock(LogOptionalFieldsSettings.class);
        when(settings.isLogSslHeaders()).thenReturn(true).getMock();
        mockGetHeader(HttpHeaders.X_SSL_CLIENT, "1");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_VERIFY, "2");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN, "subject/dn");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN, "subject/cn");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_ISSUER_DN, "issuer/dn");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_NOTBEFORE, "start");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_NOTAFTER, "end");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SESSION_ID, "session-id");
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(new RequestRecordFactory(settings));
        filter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.X_SSL_CLIENT, lastLine), is("1"));
        assertThat(getField(Fields.X_SSL_CLIENT_VERIFY, lastLine), is("2"));
        assertThat(getField(Fields.X_SSL_CLIENT_SUBJECT_DN, lastLine), is("subject/dn"));
        assertThat(getField(Fields.X_SSL_CLIENT_SUBJECT_CN, lastLine), is("subject/cn"));
        assertThat(getField(Fields.X_SSL_CLIENT_ISSUER_DN, lastLine), is("issuer/dn"));
        assertThat(getField(Fields.X_SSL_CLIENT_NOTBEFORE, lastLine), is("start"));
        assertThat(getField(Fields.X_SSL_CLIENT_NOTAFTER, lastLine), is("end"));
        assertThat(getField(Fields.X_SSL_CLIENT_SESSION_ID, lastLine), is("session-id"));
    }

    @Test
    public void testSslHeadersNotLoggedByDefault() throws Exception {
        LogOptionalFieldsSettings settings = mock(LogOptionalFieldsSettings.class);
        mockGetHeader(HttpHeaders.X_SSL_CLIENT, "1");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_VERIFY, "2");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN, "subject/dn");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN, "subject/cn");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_ISSUER_DN, "issuer/dn");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_NOTBEFORE, "start");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_NOTAFTER, "end");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SESSION_ID, "session-id");
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(new RequestRecordFactory(settings));
        filter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.X_SSL_CLIENT, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_VERIFY, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_SUBJECT_DN, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_SUBJECT_CN, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_ISSUER_DN, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_NOTBEFORE, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_NOTAFTER, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_SESSION_ID, lastLine), is(nullValue()));
    }

    @Test
    public void testSslHeadersAbsentOrUnknownValues() throws Exception {
        LogOptionalFieldsSettings settings = mock(LogOptionalFieldsSettings.class);
        when(settings.isLogSslHeaders()).thenReturn(true).getMock();
        mockGetHeader(HttpHeaders.X_SSL_CLIENT, "0");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_VERIFY, "0");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN, "-");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN, "-");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_ISSUER_DN, "");
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_NOTBEFORE, null);
        mockGetHeader(HttpHeaders.X_SSL_CLIENT_NOTAFTER, null);
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(new RequestRecordFactory(settings));
        filter.doFilter(mockReq, mockResp, mockFilterChain);
        verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
        LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
        assertEquals(Level.INFO, loggingEvent.getLevel());
        String lastLine = loggingEvent.getArgumentArray()[0].toString();
        assertThat(getField(Fields.X_SSL_CLIENT, lastLine), is("0"));
        assertThat(getField(Fields.X_SSL_CLIENT_VERIFY, lastLine), is("0"));
        assertThat(getField(Fields.X_SSL_CLIENT_SUBJECT_DN, lastLine), is("-"));
        assertThat(getField(Fields.X_SSL_CLIENT_SUBJECT_CN, lastLine), is("-"));
        assertThat(getField(Fields.X_SSL_CLIENT_ISSUER_DN, lastLine), is(""));
        assertThat(getField(Fields.X_SSL_CLIENT_NOTBEFORE, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_NOTAFTER, lastLine), is(nullValue()));
        assertThat(getField(Fields.X_SSL_CLIENT_SESSION_ID, lastLine), is(nullValue()));
    }

    protected String getField(String fieldName, String lastLine) throws JSONObjectException, IOException {
        Object fieldValue = JSON.std.mapFrom(lastLine).get(fieldName);
        return fieldValue == null ? null : fieldValue.toString();
    }
}
