package com.sap.cloud.cf.monitoring.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.sap.cloud.cf.monitoring.client.configuration.EnvUtils;

@RunWith(MockitoJUnitRunner.class)
public class CustomMetricRegistryTest {

    @Before
    public void setUp() throws Exception {
        Field inst = CustomMetricRegistry.class.getDeclaredField("instance");
        inst.setAccessible(true);
        inst.set(null, null);
    }

    @Test
    public void testCustomMetricRegistryInitializeWithoutEnvs() throws Exception {
        MetricRegistry metricRegistry = CustomMetricRegistry.get();
        MetricRegistry metricRegistry1 = CustomMetricRegistry.get();

        assertTrue(metricRegistry instanceof CustomMetricRegistry);
        assertEquals(metricRegistry, metricRegistry1);
        assertNull(((CustomMetricRegistry) metricRegistry).getReporter());
    }

    @Test
    public void testCustomMetricRegistryInitializeWithDisabledFlag() throws Exception {
        EnvUtils.setEnvs(new String[][] { getCustomMetricsEnv(false) });
        MetricRegistry metricRegistry = CustomMetricRegistry.get();

        assertNull(((CustomMetricRegistry) metricRegistry).getReporter());
    }

    @Test
    public void testCustomMetricRegistryInitializeWithEnabledFlag() throws Exception {
        EnvUtils.setEnvs(new String[][] { getCustomMetricsEnv(true) });
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
