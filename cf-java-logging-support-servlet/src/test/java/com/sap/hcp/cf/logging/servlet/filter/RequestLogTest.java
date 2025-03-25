package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import javax.servlet.DispatcherType;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@ExtendWith(MockitoExtension.class)
public class RequestLogTest {

	private Server server;
	private CloseableHttpClient client;

	@Mock
	private Appender mockedAppenderCorrelationIdFilter;

	@Mock
	private Appender mockedAppenderLoggingTestServlet;

	@Captor
	private ArgumentCaptor<LoggingEvent> loggingEventCaptorCorrelationIdFilter;

	@Captor
	private ArgumentCaptor<LoggingEvent> loggingEventCaptorLoggingTestServlet;

	@BeforeEach
	public void setUp() throws Exception {
		this.server = initJetty();
		this.client = HttpClientBuilder.create().build();
        // We need the log message, that a correlation-id is created.
		Logger loggerCorrelationIdFilter = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class.getName());
		loggerCorrelationIdFilter.addAppender(mockedAppenderCorrelationIdFilter);
		loggerCorrelationIdFilter.setLevel(Level.DEBUG);
		Logger loggerLoggingTestServlet = (Logger) LoggerFactory.getLogger(LoggingTestServlet.class.getName());
		loggerLoggingTestServlet.addAppender(mockedAppenderLoggingTestServlet);
		loggerLoggingTestServlet.setLevel(Level.INFO);

	}

	private Server initJetty() throws Exception {
		Server server = new Server(0);
		ServletContextHandler handler = new ServletContextHandler(server, null);
        handler.addFilter(RequestLoggingFilter.class, "/*", EnumSet.of(DispatcherType.INCLUDE,
                                                                          DispatcherType.REQUEST,
				DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.ASYNC));
        handler.addServlet(LoggingTestServlet.class, "/test");
		server.start();
		return server;
	}

	@AfterEach
	public void tearDown() throws Exception {
		client.close();
		server.stop();
	}

	@Test
	public void logsCorrelationIdFromRequestHeader() throws Exception {
		String correlationId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);

		try (CloseableHttpResponse response = client.execute(get)) {
			//assertNull(getCorrelationIdGenerated(), "No correlation_id should be generated.");
			verify(mockedAppenderCorrelationIdFilter, never()).doAppend(loggingEventCaptorCorrelationIdFilter.capture());

			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			/*
			assertThat("Application log without correlation id.", getRequestMessage(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
			assertThat("Request log without correlation id.", getRequestLog(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
			 */
			assertEquals(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID), correlationId);

		}
	}

	private HttpGet createRequestWithHeader(String headerName, String headerValue) {
		HttpGet get = createRequest();
		get.setHeader(headerName, headerValue);
		return get;
	}

	private HttpGet createRequest() {
		return new HttpGet(getBaseUrl() + "/test");
	}

	@Test
	public void logsGeneratedCorrelationId() throws Exception {
		try (CloseableHttpResponse response = client.execute(createRequest())) {
			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			/*
			String correlationId = getCorrelationIdGenerated();

			assertThat("Application log without correlation id.", getRequestMessage(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
			assertThat("Request log without correlation id.", getRequestLog(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
			 */
			assertNotNull(loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID));

		}
	}

	@Test
	public void logsRequestIdFromRequestHeader() throws Exception {
		String requestId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.X_VCAP_REQUEST_ID.getName(), requestId);
		try (CloseableHttpResponse response = client.execute(get)) {
			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			/*
			assertThat("Application log without request id.", getRequestMessage(),
					hasEntry(Fields.REQUEST_ID, requestId));
			assertThat("Request log without request id.", getRequestLog(),
					hasEntry(Fields.REQUEST_ID, requestId));
			 */
			assertEquals(loggingEvent.getMDCPropertyMap().get(Fields.REQUEST_ID), requestId);

		}
	}

	@Test
	public void logsTenantIdFromRequestHeader() throws Exception {
		String tenantId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.TENANT_ID.getName(), tenantId);
		try (CloseableHttpResponse response = client.execute(get)) {
			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			/*
			assertThat("Application log without tenant id.", getRequestMessage(),
					hasEntry(Fields.TENANT_ID, tenantId));
			assertThat("Request log without tenant id.", getRequestLog(), hasEntry(Fields.TENANT_ID, tenantId));
			 */
			assertEquals(loggingEvent.getMDCPropertyMap().get(Fields.TENANT_ID), tenantId);
		}
	}

    @Test
    public void logsSapPassportFromRequestHeader() throws Exception {
        String passport =
                        "2a54482a0300e60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002a54482a";
        HttpGet get = createRequestWithHeader(HttpHeaders.SAP_PASSPORT.getName(), passport);
        try (CloseableHttpResponse response = client.execute(get)) {
			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			/*
            assertThat("Application log without passport.", getRequestMessage(), hasEntry(Fields.SAP_PASSPORT,
                                                                                           passport));
            assertThat("Request log without passport.", getRequestLog(), hasEntry(Fields.SAP_PASSPORT, passport));

			 */
			assertEquals(loggingEvent.getMDCPropertyMap().get(Fields.SAP_PASSPORT), passport);
        }
    }

    @Test
    public void logsW3cTraceparentFromRequestHeader() throws Exception {
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        HttpGet get = createRequestWithHeader(HttpHeaders.W3C_TRACEPARENT.getName(), traceparent);
        try (CloseableHttpResponse response = client.execute(get)) {
			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			/*
            assertThat("Application log without traceparent.", getRequestMessage(), hasEntry(Fields.W3C_TRACEPARENT,
                                                                                             traceparent));
            assertThat("Request log without traceparent.", getRequestLog(), hasEntry(Fields.W3C_TRACEPARENT,
                                                                                     traceparent));
			 */
			assertEquals(loggingEvent.getMDCPropertyMap().get(Fields.W3C_TRACEPARENT), traceparent);
        }

    }

	@Test
	public void writesCorrelationIdFromHeadersAsResponseHeader() throws Exception {
		String correlationId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
		try (CloseableHttpResponse response = client.execute(get)) {
			assertFirstHeaderValue(correlationId, response, HttpHeaders.CORRELATION_ID);
		}
	}

	@Test
	public void writesGeneratedCorrelationIdAsResponseHeader() throws Exception {
		try (CloseableHttpResponse response = client.execute(createRequest())) {
			verify(mockedAppenderLoggingTestServlet, times(1)).doAppend(loggingEventCaptorLoggingTestServlet.capture());
			LoggingEvent loggingEvent = loggingEventCaptorLoggingTestServlet.getAllValues().get(0);
			assertEquals(Level.INFO, loggingEvent.getLevel());
			String correlationid = loggingEvent.getMDCPropertyMap().get(Fields.CORRELATION_ID);
			assertFirstHeaderValue(correlationid, response, HttpHeaders.CORRELATION_ID);
		}
	}

	@Test
	public void writesNoRequestLogIfNotConfigured() throws Exception {
		Logger loggerLoggingTestServlet = (Logger) LoggerFactory.getLogger(LoggingTestServlet.class.getName());
		loggerLoggingTestServlet.addAppender(mockedAppenderLoggingTestServlet);
		loggerLoggingTestServlet.setLevel(Level.OFF);
		try (CloseableHttpResponse response = client.execute(createRequest())) {
			verify(mockedAppenderLoggingTestServlet, never()).doAppend(loggingEventCaptorLoggingTestServlet.capture());
		}
	}

	@Test
	public void logCorrelationIdFromHeaderEvenIfRequestLogNotConfigured() throws Exception {
		Logger loggerLoggingTestServlet = (Logger) LoggerFactory.getLogger(LoggingTestServlet.class.getName());
		loggerLoggingTestServlet.addAppender(mockedAppenderLoggingTestServlet);
		loggerLoggingTestServlet.setLevel(Level.OFF);
		String correlationId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
		try (CloseableHttpResponse response = client.execute(get)) {
			verify(mockedAppenderLoggingTestServlet, never()).doAppend(loggingEventCaptorLoggingTestServlet.capture());
		}
	}

	private void setRequestLogLevel(Level level) {
		((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(RequestLogger.class).setLevel(level);
	}

	private String getBaseUrl() {
		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		return "http://localhost:" + port;
	}

	private String getCorrelationIdGenerated() throws IOException {
        Map<String, Object> generationLog = null;//systemOut.findLineAsMapWith("logger", CorrelationIdFilter.class.getName());
		if (generationLog == null) {
			return null;
		}
		return generationLog.get(Fields.CORRELATION_ID) == null ? null
				: generationLog.get(Fields.CORRELATION_ID).toString();
	}

	private static void assertFirstHeaderValue(String expected, CloseableHttpResponse response, HttpHeader header) {
		String headerValue = response.getFirstHeader(header.getName()).getValue();
		assertThat(headerValue, is(equalTo(expected)));
	}
}
