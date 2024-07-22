package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.ListAssert;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ConsoleExtension.class)
public class CustomFilterTest {

    private static ListAssert<Map<String, Object>> assertTestLogMessage(ConsoleOutput console) {
        return assertThat(console.getAllEvents()).filteredOn(
                m -> LoggingTestServlet.LOG_MESSAGE.equals(m.get(Fields.MSG)));
    }

    private static ListAssert<Map<String, Object>> assertRequestLog(ConsoleOutput console) {
        return assertThat(console.getAllEvents()).filteredOn(m -> "[SERVLET]".equals(m.get(Fields.LAYER)));
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

    @Test
    public void setsFixedTenantId(ConsoleOutput console) throws Exception {
        Server jetty = initJetty(constantTenantId("my_tenant"));
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            jetty.start();
            try (CloseableHttpResponse response = client.execute(createBasicGetRequest(jetty))) {
                assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
                assertTestLogMessage(console).anySatisfy(
                        t -> assertThat(t).containsEntry(Fields.TENANT_ID, "my_tenant"));
            }
        } finally {
            jetty.stop();
        }
    }

    /**
     * This test case addresses
     * <a href="https://github.com/SAP/cf-java-logging-support/issues/111">
     * Github issue #111</a>, by ensuring the elimination of a side-effect in {@link RequestRecordFactory}, which used
     * to overwrite custom set log fields with values extracted from http headers.
     *
     * @throws Exception
     */
    @Test
    public void usesCustomTenantIdInRequestLog(ConsoleOutput console) throws Exception {
        Filter filter = new CompositeFilter(constantTenantId("custom_tenant"), new GenerateRequestLogFilter());
        Server jetty = initJetty(filter);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            jetty.start();
            HttpGet request = createBasicGetRequest(jetty);
            request.addHeader(HttpHeaders.TENANT_ID.getName(), "other_tenant");
            try (CloseableHttpResponse response = client.execute(request)) {
                assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
                assertTestLogMessage(console).anySatisfy(
                        t -> assertThat(t).containsEntry(Fields.TENANT_ID, "custom_tenant"));
                assertRequestLog(console).anySatisfy(
                        t -> assertThat(t).containsEntry(Fields.TENANT_ID, "custom_tenant"));
            }
        } finally {
            jetty.stop();
        }
    }

    @Test
    public void canOverwriteGeneratedCorrelationId(ConsoleOutput console) throws Exception {
        Filter filter = new CompositeFilter(new CorrelationIdFilter(), constantCorrelationId("my_correlation"),
                                            new GenerateRequestLogFilter());
        Server jetty = initJetty(filter);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            jetty.start();
            try (CloseableHttpResponse response = client.execute(createBasicGetRequest(jetty))) {
                assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
                assertTestLogMessage(console).anySatisfy(
                        t -> assertThat(t).containsEntry(Fields.CORRELATION_ID, "my_correlation"));
                assertRequestLog(console).anySatisfy(
                        t -> assertThat(t).containsEntry(Fields.CORRELATION_ID, "my_correlation"));
            }
        } finally {
            jetty.stop();
        }
    }

    private Server initJetty(Filter filter) {
        Server jetty = new Server(0);
        ServletContextHandler contextHandler = new ServletContextHandler(jetty, null);
        EnumSet<DispatcherType> dispatches =
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.FORWARD,
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

}
