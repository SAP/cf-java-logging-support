package com.sap.cloud.cf.monitoring.client.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class CustomMetricsConfigurationFactoryTest {

    MockedStatic<SystemGetEnvWrapper> systemGetEnvWrapper;

    @BeforeEach
    public void gearUpSystemGetEnvWrapper() {
        systemGetEnvWrapper = mockStatic(SystemGetEnvWrapper.class);
    }

    @AfterEach
    public void tearDownSystemGetEnvWrapper() {
        systemGetEnvWrapper.close();
    }

    public static Map<String, String> convertEnvArrayIntoEnvMap(String[][] envArray) {
        return  Stream.of(envArray)
                .collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

    @Test
    public void testMatches_WithoutEnv() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] {}));
        testDefault();
    }

    @Test
    public void testMatches_WithEmptyEnv() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { { "CUSTOM_METRICS", "" } }));
        testDefault();
    }

    private void testDefault() {
        CustomMetricsConfiguration config = CustomMetricsConfigurationFactory.create();

        assertTrue(config.isEnabled());
        assertEquals(CustomMetricsConfiguration.DEFAULT_INTERVAL, config.getInterval());
        assertNotNull(config.getMetrics());
        assertTrue(config.getMetrics().isEmpty());
        assertFalse(config.metricQuantiles());
    }

    @Test
    public void testMatches_WithEnv() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { getCustomMetricsEnv("20000") }));

        CustomMetricsConfiguration config = CustomMetricsConfigurationFactory.create();

        assertFalse(config.isEnabled());
        assertEquals(20_000, config.getInterval());
        List<String> metrics = config.getMetrics();
        assertEquals(2, metrics.size());
        assertTrue(metrics.contains("timer"));
        assertTrue(metrics.contains("summary"));
        assertTrue(config.metricQuantiles());
    }

    @Test
    public void testMatches_WithShortIntervalEnv() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { getCustomMetricsEnv("1000") }));

        CustomMetricsConfiguration config = CustomMetricsConfigurationFactory.create();

        assertFalse(config.isEnabled());
        assertEquals(CustomMetricsConfiguration.DEFAULT_INTERVAL, config.getInterval());
        List<String> metrics = config.getMetrics();
        assertEquals(2, metrics.size());
        assertTrue(metrics.contains("timer"));
        assertTrue(metrics.contains("summary"));
    }

    @Test
    public void testMatches_WrongIntervalFormat() {
        assertThrows(NumberFormatException.class, () -> {
            systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][]{getCustomMetricsEnv("wronginterval")}));

            CustomMetricsConfigurationFactory.create();
        });
    }

    private static String[] getCustomMetricsEnv(String interval) {
        return new String[] { "CUSTOM_METRICS", getJson(interval) };
    }

    private static String getJson(String interval) {
        return "{\n" + //
               "    \"interval\": \"" + interval + "\",\n" + //
               "    \"enabled\": \"false\",\n" + //
               "    \"metrics\": [\"timer\", \"summary\"],\n" + //
               "    \"metricQuantiles\": \"true\"\n" + //
               "}";
    }
}
