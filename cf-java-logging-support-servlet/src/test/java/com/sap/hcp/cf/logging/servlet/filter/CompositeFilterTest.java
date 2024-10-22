package com.sap.hcp.cf.logging.servlet.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CompositeFilterTest {

    @Mock
    private Filter filter1;
    @Mock
    private Filter filter2;

    @Before
    public void setUp() throws Exception {
        Mockito.doAnswer(inv -> {
            doFilter(inv.getArguments());
            return null;
        }).when(filter1).doFilter(any(), any(), any());
        Mockito.doAnswer(inv -> {
            doFilter(inv.getArguments());
            return null;
        }).when(filter2).doFilter(any(), any(), any());
    }

    private void doFilter(Object... arguments) throws IOException, ServletException {
        ServletRequest request = (ServletRequest) arguments[0];
        ServletResponse response = (ServletResponse) arguments[1];
        FilterChain filterChain = (FilterChain) arguments[2];
        filterChain.doFilter(request, response);
    }

    @Test
    public void forwardsInitConfig() throws Exception {
        CompositeFilter filter = new CompositeFilter(filter1, filter2);
        FilterConfig config = mock(FilterConfig.class);

        filter.init(config);

        InOrder inOrder = inOrder(filter1, filter2);
        inOrder.verify(filter1).init(config);
        inOrder.verify(filter2).init(config);
    }

    @Test
    public void destroysInCorrectOrder() throws Exception {
        CompositeFilter filter = new CompositeFilter(filter1, filter2);

        filter.destroy();

        InOrder inOrder = inOrder(filter1, filter2);
        inOrder.verify(filter2).destroy();
        inOrder.verify(filter1).destroy();
    }

    @Test
    public void callsFiltersInOrder() throws Exception {
        CompositeFilter filter = new CompositeFilter(filter1, filter2);

        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        InOrder inOrder = inOrder(filter1, filter2, chain);
        inOrder.verify(filter1).doFilter(eq(request), eq(response), any());
        inOrder.verify(filter2).doFilter(eq(request), eq(response), any());
        inOrder.verify(chain).doFilter(request, response);

    }

}
