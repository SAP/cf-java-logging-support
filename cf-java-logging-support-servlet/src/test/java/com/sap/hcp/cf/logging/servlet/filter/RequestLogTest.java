package com.sap.hcp.cf.logging.servlet.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.HttpHeader;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import jakarta.servlet.DispatcherType;
import org.apache.http.HttpResponse;
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
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ConsoleExtension.class)
public class RequestLogTest {

    private Server server;
    private CloseableHttpClient client;

    private static String getResponseHeader(HttpResponse response, HttpHeader header) {
        return response.getFirstHeader(header.getName()).getValue();
    }

    private HttpGet createRequestWithHeader(String headerName, String headerValue) {
        HttpGet get = createRequest();
        get.setHeader(headerName, headerValue);
        return get;
    }

    private HttpGet createRequest() {
        return new HttpGet(getBaseUrl() + "/test");
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.server = initJetty();
        this.client = HttpClientBuilder.create().build();
        // We need the log message, that a correlation-id is created.
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(CorrelationIdFilter.class).setLevel(Level.DEBUG);

    }

    private Server initJetty() throws Exception {
        Server server = new Server(0);
        ServletContextHandler handler = new ServletContextHandler(server, null);
        handler.addFilter(RequestLoggingFilter.class, "/*",
                          EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST, DispatcherType.ERROR,
                                     DispatcherType.FORWARD, DispatcherType.ASYNC));
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
    public void logsCorrelationIdFromRequestHeader(ConsoleOutput console) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(console.getAllEvents()).allSatisfy(
                    t -> assertThat(t).containsEntry(Fields.CORRELATION_ID, correlationId));
        }
    }

    @Test
    public void logsGeneratedCorrelationId(ConsoleOutput console) throws Exception {
        try (CloseableHttpResponse response = client.execute(createRequest())) {
            Optional<String> correlationId = getCorrelationId(console);

            assertThat(correlationId).isPresent();
            assertThat(console.getAllEvents()).allSatisfy(
                    t -> assertThat(t).containsEntry(Fields.CORRELATION_ID, correlationId.get()));
        }
    }

    private Optional<String> getCorrelationId(ConsoleOutput console) {
        return console.getAllEvents().stream()
                      .filter(e -> CorrelationIdFilter.class.getName().equals(e.getOrDefault(Fields.LOGGER, "")))
                      .findFirst().map(e -> e.get(Fields.CORRELATION_ID)).map(Object::toString);
    }

    @Test
    public void logsRequestIdFromRequestHeader(ConsoleOutput console) throws Exception {
        String requestId = UUID.randomUUID().toString();
        HttpGet get = createRequestWithHeader(HttpHeaders.X_VCAP_REQUEST_ID.getName(), requestId);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(console.getAllEvents()).allSatisfy(
                    t -> assertThat(t).containsEntry(Fields.REQUEST_ID, requestId));
        }
    }

    @Test
    public void logsTenantIdFromRequestHeader(ConsoleOutput console) throws Exception {
        String tenantId = UUID.randomUUID().toString();
        HttpGet get = createRequestWithHeader(HttpHeaders.TENANT_ID.getName(), tenantId);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(console.getAllEvents()).allSatisfy(t -> assertThat(t).containsEntry(Fields.TENANT_ID, tenantId));
        }
    }

    @Test
    public void logsSapPassportFromRequestHeader(ConsoleOutput console) throws Exception {
        String passport =
                "2a54482a0300e60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002a54482a";
        HttpGet get = createRequestWithHeader(HttpHeaders.SAP_PASSPORT.getName(), passport);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(console.getAllEvents()).allSatisfy(
                    t -> assertThat(t).containsEntry(Fields.SAP_PASSPORT, passport));
        }
    }

    @Test
    public void logsW3cTraceparentFromRequestHeader(ConsoleOutput console) throws Exception {
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        HttpGet get = createRequestWithHeader(HttpHeaders.W3C_TRACEPARENT.getName(), traceparent);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(console.getAllEvents()).allSatisfy(
                    t -> assertThat(t).containsEntry(Fields.W3C_TRACEPARENT, traceparent));
        }

    }

    @Test
    public void writesCorrelationIdFromHeadersAsResponseHeader() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(response).extracting(r -> getResponseHeader(r, HttpHeaders.CORRELATION_ID))
                                .isEqualTo(correlationId);
        }
    }

    @Test
    public void writesGeneratedCorrelationIdAsResponseHeader(ConsoleOutput console) throws Exception {
        try (CloseableHttpResponse response = client.execute(createRequest())) {
            Optional<String> correlationId = getCorrelationId(console);
            assertThat(correlationId).isPresent();
            assertThat(response).extracting(r -> getResponseHeader(r, HttpHeaders.CORRELATION_ID))
                                .isEqualTo(correlationId.get());
        }
    }

    @Test
    public void writesNoRequestLogIfNotConfigured(ConsoleOutput console) throws Exception {
        setRequestLogLevel(Level.OFF);
        try (CloseableHttpResponse response = client.execute(createRequest())) {
            assertThat(console.getAllEvents()).noneSatisfy(t -> assertThat(t).containsEntry(Fields.LAYER, "[SERVLET]"))
                                              .noneSatisfy(t -> assertThat(t).containsEntry(Fields.TYPE, "request"));
        } finally {
            setRequestLogLevel(Level.INFO);
        }
    }

    @Test
    public void logCorrelationIdFromHeaderEvenIfRequestLogNotConfigured(ConsoleOutput console) throws Exception {
        setRequestLogLevel(Level.OFF);
        String correlationId = UUID.randomUUID().toString();
        HttpGet get = createRequestWithHeader(HttpHeaders.CORRELATION_ID.getName(), correlationId);
        try (CloseableHttpResponse response = client.execute(get)) {
            assertThat(console.getAllEvents()).allSatisfy(
                    t -> assertThat(t).containsEntry(Fields.CORRELATION_ID, correlationId));
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
}
