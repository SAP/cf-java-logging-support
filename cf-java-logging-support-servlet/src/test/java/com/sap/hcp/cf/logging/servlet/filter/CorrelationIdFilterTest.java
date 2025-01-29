package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

@ExtendWith(MockitoExtension.class)
public class CorrelationIdFilterTest {

    private static final String KNOWN_CORRELATION_ID = UUID.randomUUID().toString();
    private static final String KNOWN_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String KNOWN_TRACEPARENT = "00-" + KNOWN_TRACE_ID + "-00f067aa0ba902b7-01";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private ContextMapExtractor mdcExtractor;

    @BeforeEach
    public void setUp() throws Exception {
        MDC.clear();
        mdcExtractor = new ContextMapExtractor();
        doAnswer(mdcExtractor).when(chain).doFilter(request, response);
    }

    private String getExtractedCorrelationId() {
        return mdcExtractor.getField(HttpHeaders.CORRELATION_ID.getField());
    }

    @Test
    public void addsKnownCorrelationIdToMDC() throws Exception {
        when(request.getHeader(HttpHeaders.CORRELATION_ID.getName())).thenReturn(KNOWN_CORRELATION_ID);

        new CorrelationIdFilter().doFilter(request, response, chain);

        assertThat(getExtractedCorrelationId(), is(equalTo(KNOWN_CORRELATION_ID)));
    }

    @Test
    public void addsGeneratedCorrelationIdToMDC() throws Exception {

        new CorrelationIdFilter().doFilter(request, response, chain);

        assertThat(getExtractedCorrelationId(), is(not(nullValue())));
        assertThat(getExtractedCorrelationId(), is(not(equalTo(KNOWN_CORRELATION_ID))));
    }

    @Test
    public void removesCorrelationIdAfterFiltering() throws Exception {
        new CorrelationIdFilter().doFilter(request, response, chain);

        assertThat(MDC.get(HttpHeaders.CORRELATION_ID.getField()), is(nullValue()));
    }

    @Test
    public void addsKnownCorrelationIdAsResponseHeader() throws Exception {
        when(request.getHeader(HttpHeaders.CORRELATION_ID.getName())).thenReturn(KNOWN_CORRELATION_ID);

        new CorrelationIdFilter().doFilter(request, response, chain);

        verify(response).setHeader(HttpHeaders.CORRELATION_ID.getName(), KNOWN_CORRELATION_ID);
    }

    @Test
    public void doesNotAddCorrelationIdToCommittedResponse() throws Exception {
        when(response.isCommitted()).thenReturn(true);

        new CorrelationIdFilter().doFilter(request, response, chain);

        verify(response).isCommitted();
        verifyNoMoreInteractions(response);
    }

    @Test
    public void doesNotOverwriteCorrelationIdInResponse() throws Exception {
        when(response.isCommitted()).thenReturn(false);
        when(response.getHeader(HttpHeaders.CORRELATION_ID.getName())).thenReturn("preexisting-correlation-id");

        new CorrelationIdFilter().doFilter(request, response, chain);

        verify(response).isCommitted();
        verify(response).getHeader(HttpHeaders.CORRELATION_ID.getName());
        verifyNoMoreInteractions(response);
    }

    @Test
    public void usesCustomCorrelationIdHeader() throws Exception {
        HttpHeader myCorrelationIdHeader = new HttpTestHeader("my-correlationId-header", "my-correlationId-field", null,
                                                              false);
        HttpHeader myTraceparentHeader = new HttpTestHeader("my-traceparent-header", "my-traceparent-field", null,
                                                            false);
        when(request.getHeader("my-correlationId-header")).thenReturn(KNOWN_CORRELATION_ID);
        // TODO: check why this needs no stubbing anymore
        //.when(request.getHeader("my-traceparent-header")).thenReturn(KNOWN_TRACEPARENT);

        new CorrelationIdFilter(myCorrelationIdHeader, myTraceparentHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getField("my-correlationId-field"), is(equalTo(KNOWN_CORRELATION_ID)));
        verify(response).setHeader("my-correlationId-header", KNOWN_CORRELATION_ID);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void usesCustomTraceparentHeader() throws Exception {
        HttpHeader myCorrelationIdHeader = new HttpTestHeader("my-correlationId-header", "my-correlationId-field", null,
                                                              false);
        HttpHeader myTraceparentHeader = new HttpTestHeader("my-traceparent-header", "my-traceparent-field", null,
                                                            false);
        when(request.getHeader("my-traceparent-header")).thenReturn(KNOWN_TRACEPARENT);

        new CorrelationIdFilter(myCorrelationIdHeader, myTraceparentHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getField("my-correlationId-field"), is(equalTo(KNOWN_TRACE_ID)));
        verify(response).setHeader("my-correlationId-header", KNOWN_TRACE_ID);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void usesTraceparentIfCorrelationIdHeaderNotPresent() throws Exception {
        when(request.getHeader(HttpHeaders.W3C_TRACEPARENT.getName())).thenReturn(KNOWN_TRACEPARENT);

        new CorrelationIdFilter().doFilter(request, response, chain);

        assertThat(getExtractedCorrelationId(), is(equalTo(KNOWN_TRACE_ID)));
    }
}
