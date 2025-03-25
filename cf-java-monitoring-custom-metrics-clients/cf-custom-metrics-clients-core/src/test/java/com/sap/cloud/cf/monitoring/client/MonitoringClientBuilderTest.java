package com.sap.cloud.cf.monitoring.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.google.gson.Gson;
import com.sap.cloud.cf.monitoring.client.model.Metric;
import com.sap.cloud.cf.monitoring.client.model.MetricEnvelope;

@ExtendWith(MockitoExtension.class)
public class MonitoringClientBuilderTest {

    private static final int TEST_METRIC_VALUE = 30;
    private static final String TEST_METRIC_NAME = "test_metric_name";

    private static final Gson gson = new Gson();

    private final MonitoringClient client = new MonitoringClientBuilder().create();
    private final Metric metric = new Metric(TEST_METRIC_NAME, TEST_METRIC_VALUE, System.currentTimeMillis());

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream stdout;
    private PrintStream stderr;

    @BeforeEach
    public void setupStreams() {
        stdout = System.out;
        stderr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void teardownStreams() {
        System.setOut(stdout);
        System.setErr(stderr);
        System.out.append(outContent.toString());
        System.err.append(errContent.toString());
    }

    private String getLastLine() {
        String[] lines = outContent.toString().split("\n");
        return lines[lines.length - 1];
    }

    @Test
    public void testSendNoMetric() {
        assertThrows(IllegalArgumentException.class, () ->
            client.send((Metric) null));
    }

    @Test
    public void testSendNoMetrics() {
        assertThrows(IllegalArgumentException.class, () ->
            client.send((List<Metric>) null));
    }

    @Test
    public void testSendEmptyMetrics() {
        assertThrows(IllegalArgumentException.class, () ->
            client.send(new ArrayList<Metric>()));
    }

    @Test
    public void testSend() throws Exception {

        client.send(metric);

        MetricEnvelope actualMetricEnvelope = gson.fromJson(getLastLine(), MetricEnvelope.class);

        assertEquals("metrics", actualMetricEnvelope.getType());
        assertEquals(metric, actualMetricEnvelope.getMetrics().get(0));
    }
}
