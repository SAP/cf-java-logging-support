package com.sap.cloud.cf.monitoring.java;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sap.cloud.cf.monitoring.client.configuration.SystemGetEnvWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codahale.metrics.MetricRegistry;

@ExtendWith(MockitoExtension.class)
public class CustomMetricRegistryTest {

    protected MockedStatic<SystemGetEnvWrapper> systemGetEnvWrapper;

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

    @BeforeEach
    public void setUp() throws Exception {
        Field inst = CustomMetricRegistry.class.getDeclaredField("instance");
        inst.setAccessible(true);
        inst.set(null, null);
    }

    @Test
    //TODO - revisit. a CustomMetricsConfiguration is by default enabled, hence assertNotNull or set enabled = false as default
    public void testCustomMetricRegistryInitializeWithoutEnvs() throws Exception {
        MetricRegistry metricRegistry = CustomMetricRegistry.get();
        MetricRegistry metricRegistry1 = CustomMetricRegistry.get();

        assertTrue(metricRegistry instanceof CustomMetricRegistry);
        assertEquals(metricRegistry, metricRegistry1);
        assertNotNull(((CustomMetricRegistry) metricRegistry).getReporter());
    }

    @Test
    public void testCustomMetricRegistryInitializeWithDisabledFlag() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { getCustomMetricsEnv(false) }));
        MetricRegistry metricRegistry = CustomMetricRegistry.get();

        assertNull(((CustomMetricRegistry) metricRegistry).getReporter());
    }

    @Test
    public void testCustomMetricRegistryInitializeWithEnabledFlag() throws Exception {
        systemGetEnvWrapper.when(SystemGetEnvWrapper::getenv).thenReturn(convertEnvArrayIntoEnvMap(new String[][] { getCustomMetricsEnv(true) }));
        MetricRegistry metricRegistry = CustomMetricRegistry.get();

        assertNotNull(((CustomMetricRegistry) metricRegistry).getReporter());
    }

    private static String[] getCustomMetricsEnv(boolean isEnable) {
        return new String[] { "CUSTOM_METRICS", "{\n" + //
                                                "    \"interval\": \"20000\",\n" + //
                                                "    \"enabled\": \"" + isEnable + "\",\n" + //
                                                "    \"metrics\": [\"timer\", \"summary\"]\n" + //
                                                "}" };
    }
}
