package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoggingAsyncContextImplTest {

    private final ExecutorService executor = newSingleThreadExecutor();
    @Mock
    private AsyncContext wrappedContext;
    @Mock
    private RequestLogger requestLogger;
    @Mock
    private HttpServletRequest request;
    @Captor
    private ArgumentCaptor<AsyncListener> asyncListener;

    @InjectMocks
    private LoggingAsyncContextImpl testedContext;

    @BeforeEach
    public void initWrappedContext() {
        when(wrappedContext.getRequest()).thenReturn(request);
        verify(wrappedContext).addListener(asyncListener.capture());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                Future<?> future = executor.submit(runnable);
                future.get();
                return null;
            }
        }).when(wrappedContext).start(any());
    }

    @Test
    public void hasEmptyMDCWhenNoMapInRequest() throws Exception {
        Map<String, String> contextMap = new HashMap<>();
        testedContext.start(putAllContextMap(contextMap));
        assertThat(contextMap).isEmpty();
    }

    private Runnable putAllContextMap(Map<String, String> contextMap) {
        return new Runnable() {

            @Override
            public void run() {
                contextMap.putAll(MDC.getCopyOfContextMap());
            }
        };
    }

    @Test
    public void importsMDCEntriesFromRequest() throws Exception {
        Map<String, String> mdcAttributes = new HashMap<>();
        mdcAttributes.put("this-key", "this-value");
        mdcAttributes.put("that-key", "that-value");
        when(request.getAttribute(MDC.class.getName())).thenReturn(mdcAttributes);
        Map<String, String> contextMap = new HashMap<>();
        testedContext.start(putAllContextMap(contextMap));
        assertThat(contextMap).containsEntry("that-key", "that-value").containsEntry("this-key", "this-value");
    }

    @Test
    public void resetsMDCEntriesBetweenConsequtiveRuns() throws Exception {
        Map<String, String> mdcAttributes = new HashMap<>();
        mdcAttributes.put("this-key", "this-value");
        mdcAttributes.put("that-key", "that-value");
        when(request.getAttribute(MDC.class.getName())).thenReturn(mdcAttributes);
        Map<String, String> firstContextMap = new HashMap<>();
        testedContext.start(putAllContextMap(firstContextMap));
        reset(request);
        when(request.getAttribute(MDC.class.getName())).thenReturn(emptyMap());
        Map<String, String> secondContextMap = new HashMap<>();
        testedContext.start(putAllContextMap(secondContextMap));

        assertThat(firstContextMap).isNotEmpty();
        assertThat(secondContextMap).isEmpty();
    }

    @Test
    public void savesAndRestoresThreadMDC() throws Exception {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                MDC.put("initial-key", "initial-value");
            }
        }).get();
        Map<String, String> requestContextMap = new HashMap<>();
        testedContext.start(putAllContextMap(requestContextMap));
        Map<String, String> finalContextMap = new HashMap<>();
        executor.submit(new Runnable() {

            @Override
            public void run() {
                finalContextMap.putAll(MDC.getCopyOfContextMap());
            }
        }).get();

        assertThat(requestContextMap).isEmpty();
        assertThat(finalContextMap).containsEntry("initial-key", "initial-value");
    }

}
