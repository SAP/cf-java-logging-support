package com.sap.hcp.cf.logging.sample.springboot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Customization for failing authentication.
 */
@Component
public class SampleAppAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

    private static final String REALM_NAME = "Logging-Sample";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.addHeader("WWW-Authenticate", "Basic realm=" + getRealmName());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().println("HTTP Status 401 - " + authException.getMessage());
    }

    @Override
    public void afterPropertiesSet() {
        setRealmName(REALM_NAME);
        super.afterPropertiesSet();
    }
}
