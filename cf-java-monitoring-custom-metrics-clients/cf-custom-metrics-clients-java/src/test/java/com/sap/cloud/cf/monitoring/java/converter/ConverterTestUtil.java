package com.sap.cloud.cf.monitoring.java.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.sap.cloud.cf.monitoring.client.model.Metric;

public class ConverterTestUtil {
    static final String SUFFIX_COUNT = ".count";
    static final String SUFFIX_M1_RATE = ".m1_rate";
    static final String SUFFIX_M5_RATE = ".m5_rate";
    static final String SUFFIX_M15_RATE = ".m15_rate";
    static final String SUFFIX_MEAN_RATE = ".mean_rate";
    static final String SUFFIX_MAX = ".max";
    static final String SUFFIX_MIN = ".min";
    static final String SUFFIX_MEAN = ".mean";
    static final String SUFFIX_P50 = ".p50";
    static final String SUFFIX_P75 = ".p75";
    static final String SUFFIX_P95 = ".p95";
    static final String SUFFIX_P98 = ".p98";
    static final String SUFFIX_P99 = ".p99";
    static final String SUFFIX_P999 = ".p999";
    static final String SUFFIX_STDDEV = ".stddev";
    static final String SUFFIX_VALUE = ".value";

    private static final String TAG_TYPE = "type";
    private List<Metric> metrics;
    private String metricName;
    private String type;
    private long timeStamp;

    public ConverterTestUtil(List<Metric> metrics, String metricName, String type, long timeStamp) {
        this.metrics = metrics;
        this.metricName = metricName;
        this.type = type;
        this.timeStamp = timeStamp;
    }

    void checkMetric(String sufix) {
        checkMetric(sufix, null);
    }

    void checkMetric(String suffix, Double value) {
        for (Metric metric : metrics) {
            if (metric.getName().equals(metricName + suffix)) {
                assertTrue(metric.getTags().containsKey(TAG_TYPE));
                assertEquals(type, metric.getTags().get(TAG_TYPE));
                assertEquals(timeStamp, metric.getTimestamp());
                if (value != null) {
                    assertEquals(value, metric.getValue(), 0);
                }
                return;
            }
        }
        fail("Metric with name " + metricName + " does not exist in the list of converted metrics");
    }
}
