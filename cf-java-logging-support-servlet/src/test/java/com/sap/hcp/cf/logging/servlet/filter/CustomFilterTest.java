package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;

@ExtendWith(MockitoExtension.class)
public class CustomFilterTest {

    @Mock
    private Appender mockedAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

    @BeforeEach
    public void setupLogCapturing() {
        Logger loggerOfInterest = (Logger) LoggerFactory.getLogger(LoggingTestServlet.class.getName());
        loggerOfInterest.addAppender(mockedAppender);
        loggerOfInterest.setLevel(Level.INFO);
    }

    @Test
    public void setsFixedTenantId() throws Exception {
        Server jetty = initJetty(constantTenantId("my_tenant"));
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            jetty.start();
            try (CloseableHttpResponse response = client.execute(createBasicGetRequest(jetty))) {
                assertThat(response.getStatusLine().getStatusCode(), is(equalTo(200)));
                verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
                LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
                assertEquals(Level.INFO, loggingEvent.getLevel());
                assertThat(loggingEvent.getMDCPropertyMap(), hasEntry(Fields.TENANT_ID, "my_tenant"));
            }
        } finally {
            jetty.stop();
        }
    }

    /**
     * This test case addresses
     * <a href="https://github.com/SAP/cf-java-logging-support/issues/111">
     * Github issue #111</a>, by ensuring the elimination of a side-effect in
     * {@link RequestRecordFactory}, which used to overwrite custom set log
     * fields with values extracted from http headers.
     * 
     * @throws Exception
     */
    @Test
    public void usesCustomTenantIdInRequestLog() throws Exception {
        Filter filter = new CompositeFilter(constantTenantId("custom_tenant"), new GenerateRequestLogFilter());
        Server jetty = initJetty(filter);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            jetty.start();
            HttpGet request = createBasicGetRequest(jetty);
            request.addHeader(HttpHeaders.TENANT_ID.getName(), "other_tenant");
            try (CloseableHttpResponse response = client.execute(request)) {
                assertThat(response.getStatusLine().getStatusCode(), is(equalTo(200)));
                verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
                LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
                assertEquals(Level.INFO, loggingEvent.getLevel());
                assertThat(loggingEvent.getMDCPropertyMap(), hasEntry(Fields.TENANT_ID, "custom_tenant"));
            }
        } finally {
            jetty.stop();
        }
    }

    @Test
    public void canOverwriteGeneratedCorrelationId() throws Exception {
        Filter filter = new CompositeFilter(new CorrelationIdFilter(), constantCorrelationId("my_correlation"),
                                            new GenerateRequestLogFilter());
        Server jetty = initJetty(filter);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            jetty.start();
            try (CloseableHttpResponse response = client.execute(createBasicGetRequest(jetty))) {
                assertThat(response.getStatusLine().getStatusCode(), is(equalTo(200)));
                verify(mockedAppender, times(1)).doAppend(loggingEventCaptor.capture());
                LoggingEvent loggingEvent = loggingEventCaptor.getAllValues().get(0);
                assertEquals(Level.INFO, loggingEvent.getLevel());
                assertThat(loggingEvent.getMDCPropertyMap(), hasEntry(Fields.CORRELATION_ID, "my_correlation"));
            }
        } finally {
            jetty.stop();
        }
    }

    private Server initJetty(Filter filter) {
        Server jetty = new Server(0);
        ServletContextHandler contextHandler = new ServletContextHandler(jetty, null);
        EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST,
                                                        DispatcherType.ERROR, DispatcherType.FORWARD,
                                                        DispatcherType.ASYNC);
        contextHandler.addFilter(new FilterHolder(filter), "/*", dispatches);
        contextHandler.addServlet(LoggingTestServlet.class, "/test");
        return jetty;
    }

    private HttpGet createBasicGetRequest(Server jetty) {
        return new HttpGet(getBaseUrl(jetty) + "/test");
    }

    private String getBaseUrl(Server server) {
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        return "http://localhost:" + port;
    }

    private static Filter constantTenantId(String tenantId) {
        return constantField(Fields.TENANT_ID, tenantId);
    }

    private static Filter constantCorrelationId(String correlationId) {
        return constantField(Fields.CORRELATION_ID, correlationId);
    }

    private static Filter constantField(String field, String value) {
        return new AbstractLoggingFilter() {
            @Override
            protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
                MDC.put(field, value);
            }

            @Override
            protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
                MDC.remove(field);
            }
        };
    }

}
