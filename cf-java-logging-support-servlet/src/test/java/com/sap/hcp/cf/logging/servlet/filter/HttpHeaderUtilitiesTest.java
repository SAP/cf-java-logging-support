package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.sap.hcp.cf.logging.common.request.HttpHeader;

@RunWith(MockitoJUnitRunner.class)
public class HttpHeaderUtilitiesTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private HttpHeader header;

	@Before
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
	public void usesAliasOnMissingRequestHeader() throws Exception {
		HttpHeader alias = Mockito.mock(HttpHeader.class);
		when(alias.getName()).thenReturn("test_alias");
		when(header.getAliases()).thenReturn(Arrays.asList(alias));
		when(request.getHeader("test_alias")).thenReturn("test_value");

		String value = HttpHeaderUtilities.getHeaderValue(request, header);

		assertThat(value, is("test_value"));
	}
	
	@Test
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
