package com.sap.cloud.cf.monitoring.spring.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.sap.cloud.cf.monitoring.client.configuration.SystemGetEnvWrapper;
import org.junit.jupiter.api.Test;

public class CustomMetricsConfigurationFactoryTest extends CustomMetricsTestBase{

    @Test
    public void testMatches_WithoutEnv() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] {}));

        testDefault();
    }

    @Test
    public void testMatches_WithEmptyEnv() throws Exception {
        String[] CUSTOM_METRICS_ENV = new String[] { "CUSTOM_METRICS", "" };
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { CUSTOM_METRICS_ENV }));

        testDefault();
    }

    private void testDefault() {
        CustomMetricsConfiguration config = CustomMetricsConfigurationFactory.create();

        assertTrue(config.isEnabled());
        assertEquals(60 * 1000, config.getInterval());
        assertNull(config.getMetrics());
    }

    @Test
    public void testMatches_WithEnv() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { getCustomMetricsEnv() }));

        CustomMetricsConfiguration config = CustomMetricsConfigurationFactory.create();

        System.out.println(config);

        assertFalse(config.isEnabled());
        assertEquals(1000, config.getInterval());
        List<String> metrics = config.getMetrics();
        assertEquals(2, metrics.size());
        assertTrue(metrics.contains("timer"));
        assertTrue(metrics.contains("summary"));
    }

    public static String[] getCustomMetricsEnv() {
        return new String[] { "CUSTOM_METRICS", getJson() };
    }

    private static String getJson() {
        return "{\n" + //
                "    \"interval\": \"1000\",\n" + //
                "    \"enabled\": \"false\",\n" + //
                "    \"metrics\": [\"timer\", \"summary\"]\n" + //
                "}";
    }

}
