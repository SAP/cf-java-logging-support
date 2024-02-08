package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(wrapper.startAsync()).isInstanceOf(LoggingAsyncContextImpl.class);
    }

    @Test
    public void wrapsAsyncContextWithRequestResponseParameters() throws Exception {
        when(request.startAsync(request, response)).thenReturn(mock(AsyncContext.class));
        LoggingContextRequestWrapper wrapper = new LoggingContextRequestWrapper(request, null);
        assertThat(wrapper.startAsync(request, response)).isInstanceOf(LoggingAsyncContextImpl.class);
    }

}
