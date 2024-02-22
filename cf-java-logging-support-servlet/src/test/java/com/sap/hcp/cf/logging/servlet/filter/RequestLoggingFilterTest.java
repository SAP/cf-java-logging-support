package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogOptionalFieldsSettings;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.assertLastEventFields;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({ MockitoExtension.class, ConsoleExtension.class })
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

    private final HttpServletRequest mockReq = mock(HttpServletRequest.class);
    private final HttpServletResponse mockResp = mock(HttpServletResponse.class);
    private final PrintWriter mockWriter = mock(PrintWriter.class);

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
        }).when(mockReq).setAttribute(matches(MDC.class.getName()), anyMap());

        when(mockReq.getAttribute(MDC.class.getName())).thenReturn(contextMap);
    }

    @Test
    public void testSimple(ConsoleOutput console) throws IOException, ServletException {
        FilterChain mockFilterChain = mock(FilterChain.class);

        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.REQUEST_SIZE_B, -1)
                                      .hasEntrySatisfying(Fields.CORRELATION_ID,
                                                          v -> Assertions.assertThat(v.toString()).isNotBlank())
                                      .doesNotContainKey(Fields.REQUEST).doesNotContainKey(Fields.REQUEST_ID)
                                      .doesNotContainKey(Fields.REMOTE_HOST).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.CONTAINER_ID);
    }

    @Test
    public void testInputStream(ConsoleOutput console) throws IOException, ServletException {
        ServletInputStream mockStream = mock(ServletInputStream.class);

        when(mockReq.getInputStream()).thenReturn(mockStream);
        when(mockStream.read()).thenReturn(1);
        FilterChain mockFilterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                request.getInputStream().read();
            }
        };
        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.REQUEST_SIZE_B, 1).hasEntrySatisfying(Fields.CORRELATION_ID,
                                                                                                  v -> Assertions.assertThat(
                                                                                                                         v.toString())
                                                                                                                 .isNotBlank())
                                      .doesNotContainKey(Fields.REQUEST).doesNotContainKey(Fields.REQUEST_ID)
                                      .doesNotContainKey(Fields.REMOTE_HOST).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.CONTAINER_ID);
    }

    @Test
    public void testReader(ConsoleOutput console) throws IOException, ServletException {
        BufferedReader reader = new BufferedReader(new StringReader("TEST"));

        when(mockReq.getReader()).thenReturn(reader);
        FilterChain mockFilterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                request.getReader().read();
            }
        };

        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.REQUEST_SIZE_B, 4).hasEntrySatisfying(Fields.CORRELATION_ID,
                                                                                                  v -> Assertions.assertThat(
                                                                                                                         v.toString())
                                                                                                                 .isNotBlank())
                                      .doesNotContainKey(Fields.REQUEST).doesNotContainKey(Fields.REQUEST_ID)
                                      .doesNotContainKey(Fields.REMOTE_HOST).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.CONTAINER_ID);
    }

    @Test
    public void testWithActivatedOptionalFields(ConsoleOutput console) throws IOException, ServletException {
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

        assertLastEventFields(console).containsEntry(Fields.REQUEST, FULL_REQUEST)
                                      .containsEntry(Fields.CORRELATION_ID, REQUEST_ID)
                                      .containsEntry(Fields.REQUEST_ID, REQUEST_ID)
                                      .containsEntry(Fields.REMOTE_HOST, REMOTE_HOST)
                                      .containsEntry(Fields.REFERER, REFERER).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.CONTAINER_ID).doesNotContainKey(Fields.TENANT_ID);
    }

    private void mockGetHeader(HttpHeader header, String value) {
        when(mockReq.getHeader(header.getName())).thenReturn(value);
    }

    @Test
    public void testWithSuppressedOptionalFields(ConsoleOutput console) throws IOException, ServletException {
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

        assertLastEventFields(console).containsEntry(Fields.REQUEST, FULL_REQUEST)
                                      .containsEntry(Fields.CORRELATION_ID, REQUEST_ID)
                                      .containsEntry(Fields.REQUEST_ID, REQUEST_ID)
                                      .containsEntry(Fields.REMOTE_HOST, Defaults.REDACTED)
                                      .containsEntry(Fields.REFERER, Defaults.REDACTED)
                                      .doesNotContainKey(Fields.COMPONENT_ID).doesNotContainKey(Fields.CONTAINER_ID)
                                      .doesNotContainKey(Fields.TENANT_ID).doesNotContainKey(Fields.REMOTE_IP);
    }

    @Test
    public void testExplicitCorrelationId(ConsoleOutput console) throws IOException, ServletException {
        mockGetHeader(HttpHeaders.CORRELATION_ID, CORRELATION_ID);
        mockGetHeader(HttpHeaders.X_VCAP_REQUEST_ID, REQUEST_ID);
        FilterChain mockFilterChain = mock(FilterChain.class);

        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.REQUEST_ID, REQUEST_ID)
                                      .containsEntry(Fields.CORRELATION_ID, CORRELATION_ID)
                                      .doesNotContainEntry(Fields.CORRELATION_ID, REQUEST_ID)
                                      .doesNotContainKey(Fields.TENANT_ID);
    }

    @Test
    public void testExplicitW3cTraceparent(ConsoleOutput console) throws IOException, ServletException {
        mockGetHeader(HttpHeaders.W3C_TRACEPARENT, W3C_TRACEPARENT);
        FilterChain mockFilterChain = mock(FilterChain.class);

        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.W3C_TRACEPARENT, W3C_TRACEPARENT);
    }

    @Test
    public void testExplicitTenantId(ConsoleOutput console) throws IOException, ServletException {
        mockGetHeader(HttpHeaders.TENANT_ID, TENANT_ID);
        mockGetHeader(HttpHeaders.X_VCAP_REQUEST_ID, REQUEST_ID);
        FilterChain mockFilterChain = mock(FilterChain.class);

        new RequestLoggingFilter().doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.TENANT_ID, TENANT_ID);
    }

    @Test
    public void testProxyHeadersLogged(ConsoleOutput console) throws Exception {
        LogOptionalFieldsSettings settings = mock(LogOptionalFieldsSettings.class);
        when(settings.isLogSensitiveConnectionData()).thenReturn(true).getMock();
        mockGetHeader(HttpHeaders.X_CUSTOM_HOST, "custom.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_FOR, "1.2.3.4,5.6.7.8");
        mockGetHeader(HttpHeaders.X_FORWARDED_HOST, "requested.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_PROTO, "https");
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(new RequestRecordFactory(settings));

        filter.doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.X_CUSTOM_HOST, "custom.example.com")
                                      .containsEntry(Fields.X_FORWARDED_FOR, "1.2.3.4,5.6.7.8")
                                      .containsEntry(Fields.X_FORWARDED_HOST, "requested.example.com")
                                      .containsEntry(Fields.X_FORWARDED_PROTO, "https");
    }

    @Test
    public void testProxyHeadersRedactedByDefault(ConsoleOutput console) throws Exception {
        mockGetHeader(HttpHeaders.X_CUSTOM_HOST, "custom.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_FOR, "1.2.3.4,5.6.7.8");
        mockGetHeader(HttpHeaders.X_FORWARDED_HOST, "requested.example.com");
        mockGetHeader(HttpHeaders.X_FORWARDED_PROTO, "https");
        FilterChain mockFilterChain = mock(FilterChain.class);
        RequestLoggingFilter filter = new RequestLoggingFilter();

        filter.doFilter(mockReq, mockResp, mockFilterChain);

        assertLastEventFields(console).containsEntry(Fields.X_CUSTOM_HOST, Defaults.REDACTED)
                                      .containsEntry(Fields.X_FORWARDED_FOR, Defaults.REDACTED)
                                      .containsEntry(Fields.X_FORWARDED_HOST, Defaults.REDACTED)
                                      .containsEntry(Fields.X_FORWARDED_PROTO, Defaults.REDACTED);
    }

    @Test
    public void testSslHeadersLogged(ConsoleOutput console) throws Exception {
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

        assertLastEventFields(console).containsEntry(Fields.X_SSL_CLIENT, "1")
                                      .containsEntry(Fields.X_SSL_CLIENT_VERIFY, "2")
                                      .containsEntry(Fields.X_SSL_CLIENT_SUBJECT_DN, "subject/dn")
                                      .containsEntry(Fields.X_SSL_CLIENT_SUBJECT_CN, "subject/cn")
                                      .containsEntry(Fields.X_SSL_CLIENT_ISSUER_DN, "issuer/dn")
                                      .containsEntry(Fields.X_SSL_CLIENT_NOTBEFORE, "start")
                                      .containsEntry(Fields.X_SSL_CLIENT_NOTAFTER, "end")
                                      .containsEntry(Fields.X_SSL_CLIENT_SESSION_ID, "session-id");
    }

    @Test
    public void testSslHeadersNotLoggedByDefault(ConsoleOutput console) throws Exception {
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

        assertLastEventFields(console).doesNotContainKey(Fields.X_SSL_CLIENT)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_VERIFY)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_SUBJECT_DN)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_SUBJECT_CN)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_ISSUER_DN)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_NOTBEFORE)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_NOTAFTER)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_SESSION_ID);
    }

    @Test
    public void testSslHeadersAbsentOrUnknownValues(ConsoleOutput console) throws Exception {
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

        assertLastEventFields(console).containsEntry(Fields.X_SSL_CLIENT, "0")
                                      .containsEntry(Fields.X_SSL_CLIENT_VERIFY, "0")
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_SUBJECT_DN)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_SUBJECT_CN)
                                      .containsEntry(Fields.X_SSL_CLIENT_ISSUER_DN, "")
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_NOTBEFORE)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_NOTAFTER)
                                      .doesNotContainKey(Fields.X_SSL_CLIENT_SESSION_ID);
    }
}
