package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AddHttpHeadersToLogContextFilterTest {

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

    @Test
    public void addsSingleHttpHeader() throws Exception {
        when(request.getHeader("my-header")).thenReturn("my-value");
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getField("my-field")).isEqualTo("my-value");
    }

    @Test
    public void ignoresNotPropagatedHttpHeader() throws Exception {
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, false);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getContextMap()).isNullOrEmpty();
        verifyNoInteractions(request);
    }

    @Test
    public void ignoresHttpHeadersWithoutField() throws Exception {
        HttpTestHeader myHeader = new HttpTestHeader("my-header", null, null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getContextMap()).isNullOrEmpty();
    }

    @Test
    public void ignoresMissingHeaderValues() throws Exception {
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getContextMap()).isNullOrEmpty();
    }

    @Test
    public void removesFieldAfterFiltering() throws Exception {
        when(request.getHeader("my-header")).thenReturn("my-value");
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    public void addsDefaultFields() throws Exception {
        streamDefaultHeaders().map(HttpHeader::getName)
                              .forEach(n -> when(request.getHeader(n)).thenReturn(n + "-test_value"));

        new AddHttpHeadersToLogContextFilter().doFilter(request, response, chain);

        String[] fields = streamDefaultHeaders().map(HttpHeader::getField).toArray(String[]::new);
        assertThat(mdcExtractor.getContextMap()).containsKeys(fields);
    }

    private Stream<HttpHeaders> streamDefaultHeaders() {
        return HttpHeaders.propagated().stream();
    }

}
