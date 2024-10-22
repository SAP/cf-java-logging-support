package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

@RunWith(MockitoJUnitRunner.class)
public class AddHttpHeadersToLogContextFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private ContextMapExtractor mdcExtractor;

    @Before
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

        assertThat(mdcExtractor.getField("my-field"), is(equalTo("my-value")));
    }

    @Test
    public void ignoresNotPropagatedHttpHeader() throws Exception {
        when(request.getHeader("my-header")).thenReturn("my-value");
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, false);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getContextMap(), is(nullValue()));
    }

    @Test
    public void ignoresHttpHeadersWithoutField() throws Exception {
        when(request.getHeader("my-header")).thenReturn("my-value");
        HttpTestHeader myHeader = new HttpTestHeader("my-header", null, null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getContextMap(), is(nullValue()));
    }

    
    @Test
    public void ignoresMissingHeaderValues() throws Exception {
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(mdcExtractor.getContextMap(), is(nullValue()));
    }
 
    @Test
    public void removesFieldAfterFiltering() throws Exception {
        when(request.getHeader("my-header")).thenReturn("my-value");
        HttpTestHeader myHeader = new HttpTestHeader("my-header", "my-field", null, true);

        new AddHttpHeadersToLogContextFilter(myHeader).doFilter(request, response, chain);

        assertThat(MDC.getCopyOfContextMap(), either(not(hasEntry(any(String.class), any(String.class)))).or(is(nullValue())));
    }
    
    @Test
    public void addsDefaultFields() throws Exception {
        streamDefaultHeaders().map(HttpHeader::getName).forEach(n -> when(request.getHeader(n)).thenReturn(n + "-test_value"));

        new AddHttpHeadersToLogContextFilter().doFilter(request, response, chain);

        String[] fields = streamDefaultHeaders().map(HttpHeader::getField).toArray(String[]::new);
        assertThat(mdcExtractor.getContextMap().keySet(), containsInAnyOrder(fields));
    }

    private Stream<HttpHeaders> streamDefaultHeaders() {
        return HttpHeaders.propagated().stream();
    }
    
}
