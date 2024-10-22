package com.sap.cloud.cf.monitoring.spring.configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CustomMetricsConditionTest {
    private CustomMetricsCondition condition;

    @Mock
    private Environment environment;

    @Mock
    private ConditionContext context;

    @Before
    public void setup() {
        condition = new CustomMetricsCondition();
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    public void testMatches_WithAllEnvs() throws Exception {
        when(environment.getProperty("VCAP_SERVICES")).thenReturn("{\"application-logs\": [{}]}");

        boolean matches = condition.matches(context, null);

        assertTrue("Should send metrics on binding to application-logs", matches);
    }

    @Test
    public void testMatches_Without_VCAP_SERVICES() throws Exception {
        boolean matches = condition.matches(context, null);

        assertFalse("Should not send metrics when not running in CF.", matches);
    }

    @Test
    public void testMatches_Without_Binding() throws Exception {
        when(environment.getProperty("VCAP_SERVICES")).thenReturn("");

        boolean matches = condition.matches(context, null);

        assertFalse("Should not send metrics if not bound to application-logs", matches);
    }
}
