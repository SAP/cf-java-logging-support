package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpHeaderUtilitiesTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpHeader header;

    @BeforeEach
    public void setUp() {
        when(header.getName()).thenReturn("test_header");

    }

    @Test
    public void readsHeaderFromRequest() throws Exception {
        when(request.getHeader("test_header")).thenReturn("test_value");

        String value = HttpHeaderUtilities.getHeaderValue(request, header);

        assertThat(value).isEqualTo("test_value");
    }

    @Test
    public void readsHeaderFromResponse() throws Exception {
        when(response.getHeader("test_header")).thenReturn("test_value");

        String value = HttpHeaderUtilities.getHeaderValue(response, header);

        assertThat(value).isEqualTo("test_value");
    }

    @Test
    public void returnsDefaultOnMissingRequestHeader() throws Exception {
        String value = HttpHeaderUtilities.getHeaderValue(request, header, "default_value");

        assertThat(value).isEqualTo("default_value");
    }

    @Test
    public void returnsDefaultOnMissingResponseHeader() throws Exception {
        String value = HttpHeaderUtilities.getHeaderValue(response, header, "default_value");

        assertThat(value).isEqualTo("default_value");
    }

    @Test
    public void usesAliasOnMissingRequestHeader() throws Exception {
        HttpHeader alias = Mockito.mock(HttpHeader.class);
        when(alias.getName()).thenReturn("test_alias");
        when(header.getAliases()).thenReturn(List.of(alias));
        when(request.getHeader("test_header")).thenReturn(null);
        when(request.getHeader("test_alias")).thenReturn("test_value");

        String value = HttpHeaderUtilities.getHeaderValue(request, header);

        assertThat(value).isEqualTo("test_value");
    }

    @Test
    public void usesAliasOnMissingResponseHeader() throws Exception {
        HttpHeader alias = Mockito.mock(HttpHeader.class);
        when(alias.getName()).thenReturn("test_alias");
        when(header.getAliases()).thenReturn(List.of(alias));
        when(response.getHeader("test_header")).thenReturn(null);
        when(response.getHeader("test_alias")).thenReturn("test_value");

        String value = HttpHeaderUtilities.getHeaderValue(response, header);

        assertThat(value).isEqualTo("test_value");
    }

    @Test
    public void ignoresUnknownAliasOnMissingRequestHeader() throws Exception {
        HttpHeader alias = Mockito.mock(HttpHeader.class);
        when(alias.getName()).thenReturn("test_alias");
        when(header.getAliases()).thenReturn(List.of(alias));

        String value = HttpHeaderUtilities.getHeaderValue(request, header, "default_value");

        assertThat(value).isEqualTo("default_value");
    }

    @Test
    public void ignoresUnknownAliasOnMissingResponseHeader() throws Exception {
        HttpHeader alias = Mockito.mock(HttpHeader.class);
        when(alias.getName()).thenReturn("test_alias");
        when(header.getAliases()).thenReturn(List.of(alias));

        String value = HttpHeaderUtilities.getHeaderValue(response, header, "default_value");

        assertThat(value).isEqualTo("default_value");
    }

}
