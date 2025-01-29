package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoggingContextRequestWrapperTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Test
	public void wrapsAsyncContext() throws Exception {
		when(request.startAsync()).thenReturn(mock(AsyncContext.class));
		LoggingContextRequestWrapper wrapper = new LoggingContextRequestWrapper(request, null);
		assertThat(wrapper.startAsync(), instanceOf(LoggingAsyncContextImpl.class));
	}

	@Test
	public void wrapsAsyncContextWithRequestResponseParameters() throws Exception {
		when(request.startAsync(request, response)).thenReturn(mock(AsyncContext.class));
		LoggingContextRequestWrapper wrapper = new LoggingContextRequestWrapper(request, null);
		assertThat(wrapper.startAsync(request, response), instanceOf(LoggingAsyncContextImpl.class));
	}

}
