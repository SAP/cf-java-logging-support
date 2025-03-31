package com.sap.hcp.cf.logging.servlet.dynlog.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.servlet.filter.DynamicLogLevelFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.sap.hcp.cf.logging.servlet.dynlog.jwt.PemUtils.createPublicKeyPem;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({ SystemStubsExtension.class, ConsoleExtension.class })
public class DynamicLogLevelServletTest {

    @SystemStub
    private EnvironmentVariables environment;

    private Algorithm algorithm;
    private Server server;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        environment.set("DYN_LOG_LEVEL_KEY", createPublicKeyPem(keyPair.getPublic()));
        algorithm = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        server = new Server(0);
        ServletContextHandler handler = new ServletContextHandler(server, null);
        handler.addFilter(DynamicLogLevelFilter.class, "/*",
                          EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST, DispatcherType.ERROR,
                                     DispatcherType.FORWARD, DispatcherType.ASYNC));
        handler.addServlet(TestServlet.class, "/test");
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    void canSetLogLevelDynamically(ConsoleOutput stdout) throws IOException {
        String jwt = JWT.create().withIssuer(getClass().getSimpleName()).withIssuedAt(Date.from(Instant.now()))
                        .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES))).withClaim("level", "DEBUG")
                        .withClaim("packages", "com.sap.hcp").sign(algorithm);
        try (var client = HttpClientBuilder.create().setDefaultHeaders(List.of(new BasicHeader("SAP-LOG-LEVEL", jwt)))
                                           .build()) {
            client.execute(new HttpGet(getBaseUrl() + "/test?test=canSetLogLevelDynamically"));
        }
        Map<String, Object> logEvent =
                stdout.getAllEvents().stream().filter(e -> "canSetLogLevelDynamically".equals(e.get("msg"))).findFirst()
                      .orElseThrow(() -> new AssertionError("no log message found"));
        assertThat(logEvent).containsEntry("level", "DEBUG").containsEntry("logger", TestServlet.class.getName())
                            .containsEntry("dynamic_log_level", "DEBUG");
    }

    @Test
    void noServletLogWithoutHeader(ConsoleOutput stdout) throws Exception {
        try (var client = HttpClientBuilder.create().build()) {
            client.execute(new HttpGet(getBaseUrl() + "/test?test=canSetLogLevelDynamically"));
        }
        assertThat(stdout.getAllEvents().stream().filter(e -> "canSetLogLevelDynamically".equals(e.get("msg")))
                         .findFirst()).isEmpty();

    }

    private String getBaseUrl() {
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        return "http://localhost:" + port;
    }

    public static class TestServlet extends HttpServlet {

        private static final Logger LOG = LoggerFactory.getLogger(TestServlet.class);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            LOG.debug(req.getParameterMap().get("test")[0]);
        }
    }
}

