package com.sap.cloud.cf.monitoring.spring.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
public class CustomMetricsConditionTest {
    private CustomMetricsCondition condition;

    @Mock
    private Environment environment;

    @Mock
    private ConditionContext context;

    @BeforeEach
    public void setup() {
        condition = new CustomMetricsCondition();
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    public void testMatches_WithAllEnvs() throws Exception {
        when(environment.getProperty("VCAP_SERVICES")).thenReturn("{\"application-logs\": [{}]}");

        boolean matches = condition.matches(context, null);

        assertTrue(matches, "Should send metrics on binding to application-logs");
    }

    @Test
    public void testMatches_Without_VCAP_SERVICES() throws Exception {
        boolean matches = condition.matches(context, null);

        assertFalse(matches, "Should not send metrics when not running in CF.");
    }

    @Test
    public void testMatches_Without_Binding() throws Exception {
        when(environment.getProperty("VCAP_SERVICES")).thenReturn("");

        boolean matches = condition.matches(context, null);

        assertFalse(matches, "Should not send metrics if not bound to application-logs");
    }
}
