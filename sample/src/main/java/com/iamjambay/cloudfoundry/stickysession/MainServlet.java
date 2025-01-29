package com.iamjambay.cloudfoundry.stickysession;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.cf.monitoring.client.configuration.SystemGetEnvWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.sample.Fibonacci;
import com.sap.hcp.cf.sample.PickGreeting;

public class MainServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CF_INSTANCE_INDEX = "CF_INSTANCE_INDEX";
    private final Logger logger = LoggerFactory.getLogger(MainServlet.class);
    private static final String STACKTRACE = "/stacktrace";

    public MainServlet() {
    }

    private void printHeaders(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
        writer.println("<h2>Request Headers:</h2><p>");
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            writer.print("<i>" + header + "</i>: ");
            Enumeration<String> values = request.getHeaders(header);
            while (values.hasMoreElements()) {
                writer.print(values.nextElement() + ";");
            }
            writer.println("<br>");
        }
    }

    protected void printCookies(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        boolean sticky = false;

        if ("true".equals(request.getParameter("sticky"))) {
            request.getSession(true);
            sticky = true;
        }

        if (cookies != null) {
            writer.println("<h2>Cookies:</h2>");
            for (Cookie cookie: cookies) {
                String name = cookie.getName();
                String value = cookie.getValue();
                if ("__VCAP_ID__".equals(name)) {
                    sticky = true;
                }
                writer.println("  Name: " + name + "<br/>");
                writer.println("  Value: " + value + "<br/>");
            }
            writer.println("<br/><br/>");
        }

        if (sticky) {
            writer.println("Sticky session is enabled. Refreshing the browser should keep routing to the same app instance.<br/><br/>");
        } else {
            writer.println("Sticky session is NOT enabled. Refreshing the browser should route to random app instances.<br/><br/><a href='?sticky=true'>start a sticky session</a>" +
                           "<br/><br/>");
        }
    }

    protected String getInstance(HttpServletRequest request) {
        String instanceIdx = System.getenv(CF_INSTANCE_INDEX);
        if (instanceIdx == null) {
            instanceIdx = "???";
        }
        return instanceIdx;
    }

    protected void printEnv(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
        writer.println("<h2>Environment Variables:</h2><p>");
        for (Entry<String, String> evar: SystemGetEnvWrapper.getenv().entrySet()) {
            writer.println("<i>" + evar.getKey() + "</i>: " + evar.getValue() + "<br/>");
        }
        writer.println("</p>");
    }

    protected void printGreeting(PrintWriter writer) throws IOException {
        writer.println("<h2>Greeting of the day: " + PickGreeting.pick() + "</h2>");
    }

    private void printFibonacci(PrintWriter writer, String qs) {
        try {
            BigInteger result = new Fibonacci().compute(BigInteger.valueOf(Long.parseLong(qs)));
            writer.println("<h2>Fibonacci Number Computation</h2>Fibonacci number " + qs + " is " + result.toString());
        } catch (Exception ex) {
            logger.error("Cannot compute fibonacci number for " + qs, ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                   IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        String servletPath = request.getPathInfo();
        if (STACKTRACE.equals(servletPath)) {
            response.setStatus(500);
            try {
                String foo = null;
                foo.length();
            } catch (Exception ex) {
                logger.error("Exception occured", ex);
                ex.printStackTrace(new PrintStream(response.getOutputStream()));
                return;
            }
        }
        PrintWriter writer = response.getWriter();
        if (servletPath.length() > 1) {
            response.setStatus(404);
            response.setContentLength(0);
            return;
        }
        response.setStatus(200);

        writer.println("<h1>Sticky Session Example App on Instance " + getInstance(request) + "</h1>");

        String qs = request.getQueryString();
        if (qs != null) {
            printFibonacci(writer, qs);
        }
        printGreeting(writer);
        printHeaders(request, writer);
        printCookies(request, writer);
        printEnv(request, writer);
        writer.flush();
        writer.close();
    }
}
