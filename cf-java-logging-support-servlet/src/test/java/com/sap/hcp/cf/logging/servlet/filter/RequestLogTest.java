package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import javax.servlet.DispatcherType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class RequestLogTest {

    @Rule
	public SystemOutRule systemOut = new SystemOutRule();

	private Server server;
	private CloseableHttpClient client;

	@Before
	public void setUp() throws Exception {
		this.server = initJetty();
		this.client = HttpClientBuilder.create().build();
        // We need the log message, that a correlation-id is created.
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(CorrelationIdFilter.class).setLevel(Level.DEBUG);

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

	@After
	public void tearDown() throws Exception {
		client.close();
		server.stop();
	}

	@Test
	public void logsCorrelationIdFromRequestHeader() throws Exception {
		String correlationId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
		try (CloseableHttpResponse response = client.execute(get)) {
			assertNull("No correlation_id should be generated.", getCorrelationIdGenerated());

			assertThat("Application log without correlation id.", getRequestMessage(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
			assertThat("Request log without correlation id.", getRequestLog(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
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
			String correlationId = getCorrelationIdGenerated();

			assertThat("Application log without correlation id.", getRequestMessage(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
			assertThat("Request log without correlation id.", getRequestLog(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
		}
	}

	@Test
	public void logsRequestIdFromRequestHeader() throws Exception {
		String requestId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.X_VCAP_REQUEST_ID.getName(), requestId);
		try (CloseableHttpResponse response = client.execute(get)) {
			assertThat("Application log without request id.", getRequestMessage(),
					hasEntry(Fields.REQUEST_ID, requestId));
			assertThat("Request log without request id.", getRequestLog(),
					hasEntry(Fields.REQUEST_ID, requestId));
		}
	}

	@Test
	public void logsTenantIdFromRequestHeader() throws Exception {
		String tenantId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.TENANT_ID.getName(), tenantId);
		try (CloseableHttpResponse response = client.execute(get)) {
			assertThat("Application log without tenant id.", getRequestMessage(),
					hasEntry(Fields.TENANT_ID, tenantId));
			assertThat("Request log without tenant id.", getRequestLog(), hasEntry(Fields.TENANT_ID, tenantId));
		}
	}

    @Test
    public void logsSapPassportFromRequestHeader() throws Exception {
        String passport =
                        "2a54482a0300e60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002a54482a";
        HttpGet get = createRequestWithHeader(HttpHeaders.SAP_PASSPORT.getName(), passport);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat("Application log without passport.", getRequestMessage(), hasEntry(Fields.SAP_PASSPORT,
                                                                                           passport));
            assertThat("Request log without passport.", getRequestLog(), hasEntry(Fields.SAP_PASSPORT, passport));
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
			assertFirstHeaderValue(getCorrelationIdGenerated(), response, HttpHeaders.CORRELATION_ID);
		}
	}

	@Test
	public void writesNoRequestLogIfNotConfigured() throws Exception {
		setRequestLogLevel(Level.OFF);
		try (CloseableHttpResponse response = client.execute(createRequest())) {
			assertThat(getRequestLog().entrySet(), is(empty()));
		} finally {
			setRequestLogLevel(Level.INFO);
		}
	}

	@Test
	public void logCorrelationIdFromHeaderEvenIfRequestLogNotConfigured() throws Exception {
		setRequestLogLevel(Level.OFF);
		String correlationId = UUID.randomUUID().toString();
		HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
		try (CloseableHttpResponse response = client.execute(get)) {
			assertThat("Application log without correlation id.", getRequestMessage(),
					hasEntry(Fields.CORRELATION_ID, correlationId));
		} finally {
			setRequestLogLevel(Level.INFO);
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
        Map<String, Object> generationLog = systemOut.findLineAsMapWith("logger", CorrelationIdFilter.class.getName());
		if (generationLog == null) {
			return null;
		}
		return generationLog.get(Fields.CORRELATION_ID) == null ? null
				: generationLog.get(Fields.CORRELATION_ID).toString();
	}

	private Map<String, Object> getRequestMessage() throws IOException {
        return systemOut.findLineAsMapWith("msg", LoggingTestServlet.LOG_MESSAGE);
	}

    private Map<String, Object> getRequestLog() throws IOException {
		return systemOut.findLineAsMapWith("layer", "[SERVLET]");
	}

	private static void assertFirstHeaderValue(String expected, CloseableHttpResponse response, HttpHeader header) {
		String headerValue = response.getFirstHeader(header.getName()).getValue();
		assertThat(headerValue, is(equalTo(expected)));
	}
}
