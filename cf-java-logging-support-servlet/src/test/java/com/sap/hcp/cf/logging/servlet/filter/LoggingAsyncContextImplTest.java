package com.sap.hcp.cf.logging.servlet.filter;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;

@RunWith(MockitoJUnitRunner.class)
public class LoggingAsyncContextImplTest {

	@Mock
	private AsyncContext wrappedContext;

	@Mock
	private RequestLogger requestLogger;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	private ExecutorService executor = newSingleThreadExecutor();

	@Captor
	private ArgumentCaptor<AsyncListener> asyncListener;

	@InjectMocks
	private LoggingAsyncContextImpl testedContext;

	@Before
	public void initWrappedContext() {
		when(wrappedContext.getRequest()).thenReturn(request);
		//TODO: check why this needs not to be mocked anymore
		//when(wrappedContext.getResponse()).thenReturn(response);
		verify(wrappedContext).addListener(asyncListener.capture());
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Runnable runnable = (Runnable) invocation.getArguments()[0];
				Future<?> future = executor.submit(runnable);
				future.get();
				return null;
			}
		}).when(wrappedContext).start(any(Runnable.class));
	}

	@Test
	public void hasEmptyMDCWhenNoMapInRequest() throws Exception {
		Map<String, String> contextMap = new HashMap<>();
		testedContext.start(putAllContextMap(contextMap));
		assertThat(contextMap.entrySet(), is(empty()));
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
		assertThat(contextMap, both(hasEntry("that-key", "that-value")).and(hasEntry("this-key", "this-value")));
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

		assertThat(firstContextMap.entrySet(), is(not(empty())));
		assertThat(secondContextMap.entrySet(), is(empty()));
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

		assertThat(requestContextMap.entrySet(), is(empty()));
		assertThat(finalContextMap, hasEntry("initial-key", "initial-value"));
	}

}
