package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sap.hcp.cf.logging.common.request.HttpHeader;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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

		assertThat(value, is("test_value"));
	}

	@Test
	public void readsHeaderFromResponse() throws Exception {
		when(response.getHeader("test_header")).thenReturn("test_value");

		String value = HttpHeaderUtilities.getHeaderValue(response, header);

		assertThat(value, is("test_value"));
	}

	@Test
	public void returnsDefaultOnMissingRequestHeader() throws Exception {
		String value = HttpHeaderUtilities.getHeaderValue(request, header, "default_value");

		assertThat(value, is("default_value"));
	}

	@Test
	public void returnsDefaultOnMissingResponseHeader() throws Exception {
		String value = HttpHeaderUtilities.getHeaderValue(response, header, "default_value");

		assertThat(value, is("default_value"));
	}

	@Test
	@MockitoSettings(strictness = Strictness.LENIENT)
	public void usesAliasOnMissingRequestHeader() throws Exception {
		HttpHeader alias = Mockito.mock(HttpHeader.class);
		when(alias.getName()).thenReturn("test_alias");
		when(header.getAliases()).thenReturn(Arrays.asList(alias));
		when(request.getHeader("test_alias")).thenReturn("test_value");

		String value = HttpHeaderUtilities.getHeaderValue(request, header);

		assertThat(value, is("test_value"));
	}
	
	@Test
	@MockitoSettings(strictness = Strictness.LENIENT)
	public void usesAliasOnMissingResponseHeader() throws Exception {
		HttpHeader alias = Mockito.mock(HttpHeader.class);
		when(alias.getName()).thenReturn("test_alias");
		when(header.getAliases()).thenReturn(Arrays.asList(alias));
		when(response.getHeader("test_alias")).thenReturn("test_value");

		String value = HttpHeaderUtilities.getHeaderValue(response, header);

		assertThat(value, is("test_value"));
	}

	@Test
	public void ignoresUnknownAliasOnMissingRequestHeader() throws Exception {
		HttpHeader alias = Mockito.mock(HttpHeader.class);
		when(alias.getName()).thenReturn("test_alias");
		when(header.getAliases()).thenReturn(Arrays.asList(alias));

		String value = HttpHeaderUtilities.getHeaderValue(request, header, "default_value");

		assertThat(value, is("default_value"));
	}

	@Test
	public void ignoresUnknownAliasOnMissingResponseHeader() throws Exception {
		HttpHeader alias = Mockito.mock(HttpHeader.class);
		when(alias.getName()).thenReturn("test_alias");
		when(header.getAliases()).thenReturn(Arrays.asList(alias));

		String value = HttpHeaderUtilities.getHeaderValue(response, header, "default_value");

		assertThat(value, is("default_value"));
	}

}
