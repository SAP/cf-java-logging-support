package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Value;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord;

@ExtendWith(MockitoExtension.class)
public class RequestLoggerTest {

	@Mock
	private ContentLengthTrackingResponseWrapper responseWrapper;

	@Mock
	private RequestRecord requestRecord;

	@Mock
	private HttpServletRequest httpRequest;

	@Mock
	private HttpServletResponse httpResponse;

	@Captor
	private ArgumentCaptor<Value> valueCaptor;

	@Mock
	private Appender mockedAppender;

	@Captor
	private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

	private RequestLogger createLoggerWithoutResponse(HttpServletResponse response) {
		return new RequestLogger(requestRecord, httpRequest, response);
	}

	@Test
	public void stopsRequestRecord() throws Exception {
		createLoggerWithoutResponse(httpResponse).logRequest();
		verify(requestRecord).stop();
	}

	@Test
	public void addsHttpStatusAsValue() throws Exception {
		when(httpResponse.getStatus()).thenReturn(123);
		createLoggerWithoutResponse(httpResponse).logRequest();
		verify(requestRecord).addValue(eq(Fields.RESPONSE_STATUS), valueCaptor.capture());
		assertThat(valueCaptor.getValue().asLong(), is(123L));
	}

	@Test
	@MockitoSettings(strictness = Strictness.LENIENT)
	public void addsResponseContentTypeAsTag() throws Exception {
		mockGetHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.test");
		createLoggerWithoutResponse(httpResponse).logRequest();
		verify(requestRecord).addTag(Fields.RESPONSE_CONTENT_TYPE, "application/vnd.test");
	}

	private void mockGetHeader(HttpHeaders header, String value) {
		//doReturn(value).when(httpResponse.getHeader(header.getName()));//.thenReturn(value);
		when(httpResponse.getHeader(header.getName())).thenReturn(value);
	}

	@Test
	public void addsRequestContentLengthAsValue() throws Exception {
		when(httpRequest.getContentLength()).thenReturn(12345);
		createLoggerWithoutResponse(httpResponse).logRequest();
		verify(requestRecord).addValue(eq(Fields.REQUEST_SIZE_B), valueCaptor.capture());
		assertThat(valueCaptor.getValue().asLong(), is(12345L));
	}

	@Test
	public void addsResponseContentLengthAsValueFromHeaderIfAvailable() throws Exception {
		mockGetHeader(HttpHeaders.CONTENT_LENGTH, "1234");
		createLoggerWithoutResponse(httpResponse).logRequest();
		verify(requestRecord).addValue(eq(Fields.RESPONSE_SIZE_B), valueCaptor.capture());
		verifyNoMoreInteractions(responseWrapper);
		assertThat(valueCaptor.getValue().asLong(), is(1234L));
	}

	@Test
	public void addsResponseContentLengthAsValueFromWrapperAsFAllback() throws Exception {
		when(responseWrapper.getContentLength()).thenReturn(1234L);
		createLoggerWithoutResponse(responseWrapper).logRequest();
		verify(requestRecord).addValue(eq(Fields.RESPONSE_SIZE_B), valueCaptor.capture());
		assertThat(valueCaptor.getValue().asLong(), is(1234L));
	}

	@Test
	public void writesRequestLogWithMDCEntries() throws Exception {

		Logger loggerOfInterest = (Logger) LoggerFactory.getLogger(RequestLogger.class.getName());
		loggerOfInterest.addAppender(mockedAppender);
		loggerOfInterest.setLevel(Level.INFO);

		Map<String, String> mdcAttributes = new HashMap<>();
		mdcAttributes.put("this-key", "this-value");
		mdcAttributes.put("that-key", "that-value");
		when(httpRequest.getAttribute(MDC.class.getName())).thenReturn(mdcAttributes);
		createLoggerWithoutResponse(httpResponse).logRequest();

		verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
		LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
		assertEquals(Level.INFO, loggingEvent.getLevel());
		assertThat(loggingEvent.getMDCPropertyMap(), hasEntry("this-key", "this-value"));
		assertThat(loggingEvent.getMDCPropertyMap(), hasEntry("that-key", "that-value"));

	}

}
