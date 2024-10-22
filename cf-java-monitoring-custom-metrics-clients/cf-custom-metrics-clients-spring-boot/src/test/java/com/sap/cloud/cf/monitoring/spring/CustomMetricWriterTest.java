package com.sap.cloud.cf.monitoring.spring;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sap.cloud.cf.monitoring.client.MonitoringClient;
import com.sap.cloud.cf.monitoring.client.configuration.CustomMetricsConfiguration;
import com.sap.cloud.cf.monitoring.client.configuration.CustomMetricsConfigurationFactory;
import com.sap.cloud.cf.monitoring.client.exceptions.MonitoringClientException;
import com.sap.cloud.cf.monitoring.client.model.Metric;

import io.micrometer.core.instrument.Clock;

@RunWith(MockitoJUnitRunner.class)
public class CustomMetricWriterTest {

    @Mock
    private MonitoringClient client;

    @Test
    public void testWriter_disabled() throws Exception {
        EnvUtils.setEnvs(new String[][] { getCustomMetricsEnv() });

        initMockedWriter(CustomMetricsConfigurationFactory.create());
    }

    @Test(expected = StartIsCalledException.class)
    public void testWriter_enabled() throws Exception {
        EnvUtils.setEnvs(new String[][] {});

        initMockedWriter(CustomMetricsConfigurationFactory.create());
    }

    @Test
    public void testPublish_successfullyWithoutWhitelistedMetrics() {
        CustomMetricWriter writer = createWriter();
        writer.timer("timer");

        writer.publish();

        checkMetricsAreSent();
    }

    @Test
    public void testPublish_successfullyWithWhitelistedMetrics() throws Exception {
        EnvUtils.setEnvs(new String[][] { getCustomMetricsEnv() });
        CustomMetricWriter writer = createWriter();
        writer.timer("timer");
        writer.timer("secondTimer");

        writer.publish();

        checkMetricsAreSent();
    }

    @Test
    public void testPublish_withException() throws Exception {
        doThrow(MonitoringClientException.class).when(client).send(anyList());
        CustomMetricWriter writer = createWriter();
        writer.timer("timer");

        writer.publish();

        verify(client, times(2)).send(anyList());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void checkMetricsAreSent() {
        Class<ArrayList<Metric>> listClass = (Class) ArrayList.class;
        ArgumentCaptor<ArrayList<Metric>> argument = ArgumentCaptor.forClass(listClass);

        verify(client).send(argument.capture());
        assertEquals(4, argument.getValue().size());
        for (Metric metric : argument.getValue()) {
            assertEquals("timer", metric.getName());
        }
    }

    private CustomMetricWriter createWriter() {
        return new CustomMetricWriter(CustomMetricsConfigurationFactory.create(), Clock.SYSTEM, client);
    }

    public CustomMetricWriter initMockedWriter(CustomMetricsConfiguration config) {
        return new CustomMetricWriter(config, Clock.SYSTEM, client) {
            @Override
            public void start() {
                throw new StartIsCalledException();
            }
        };
    }

    class StartIsCalledException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static String[] getCustomMetricsEnv() {
        return new String[] { "CUSTOM_METRICS", getJson() };
    }

    private static String getJson() {
        return "{\n" + //
                "    \"interval\": \"20000\",\n" + //
                "    \"enabled\": \"false\",\n" + //
                "    \"metrics\": [\"timer\", \"summary\"]\n" + //
                "}";
    }
}