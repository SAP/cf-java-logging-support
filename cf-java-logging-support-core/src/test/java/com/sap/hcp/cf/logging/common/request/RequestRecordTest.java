package com.sap.hcp.cf.logging.common.request;

import com.fasterxml.jackson.jr.ob.JSON;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.DoubleValue;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestRecordTest {

    private static final Clock FIXED_CLOCK_EPOCH = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    private static final Duration RESPONSE_DELAY = Duration.ofMillis(150);

    private static AbstractDoubleAssert<?> assertLatency(Object v) {
        return assertThat(Double.valueOf(v.toString()));
    }

    private static MapAssert<String, Object> assertRequestRecord(RequestRecord record) throws IOException {
        return Assertions.assertThat(JSON.std.mapFrom(record.toString()));
    }

    @BeforeEach
    public void resetRequestRecordClock() {
        setRequestRecordClock(FIXED_CLOCK_EPOCH);
    }

    @Test
    public void testDefaults() throws IOException {
        String layer = "testDefaults";
        RequestRecord requestRecord = new RequestRecord(layer);
        assertRequestRecord(requestRecord).containsEntry(Fields.DIRECTION, Direction.IN.toString())
                                          .containsEntry(Fields.LAYER, layer).containsEntry(Fields.REQUEST_SIZE_B, -1)
                                          .containsEntry(Fields.RESPONSE_SIZE_B, -1)
                                          .containsKey(Fields.REQUEST_RECEIVED_AT)
                                          .hasEntrySatisfying(Fields.RESPONSE_TIME_MS,
                                                              v -> assertLatency(v).isGreaterThanOrEqualTo(0d))
                                          .containsEntry(Fields.REQUEST, Defaults.UNKNOWN)
                                          .containsEntry(Fields.REMOTE_IP, Defaults.UNKNOWN)
                                          .containsEntry(Fields.REMOTE_HOST, Defaults.UNKNOWN)
                                          .containsEntry(Fields.PROTOCOL, Defaults.UNKNOWN)
                                          .containsEntry(Fields.METHOD, Defaults.UNKNOWN)
                                          .containsEntry(Fields.REMOTE_IP, Defaults.UNKNOWN)
                                          .containsEntry(Fields.RESPONSE_CONTENT_TYPE, Defaults.UNKNOWN)
                                          .containsEntry(Fields.REMOTE_HOST, Defaults.UNKNOWN)
                                          .doesNotContainKey(Fields.REFERER).doesNotContainKey(Fields.X_FORWARDED_FOR)
                                          .doesNotContainKey(Fields.REMOTE_PORT);
    }

    @Test
    public void testNonDefaults() throws IOException {
        String layer = "testNonDefaults";
        String NON_DEFAULT = "NON_DEFAULT";
        RequestRecord requestRecord = new RequestRecord(layer);
        requestRecord.addValue(Fields.RESPONSE_TIME_MS, new DoubleValue(0.0));
        requestRecord.addTag(Fields.REQUEST, NON_DEFAULT);
        requestRecord.addTag(Fields.REMOTE_IP, NON_DEFAULT);
        requestRecord.addTag(Fields.REMOTE_HOST, NON_DEFAULT);
        requestRecord.addTag(Fields.PROTOCOL, NON_DEFAULT);
        requestRecord.addTag(Fields.METHOD, NON_DEFAULT);
        requestRecord.addTag(Fields.REMOTE_IP, NON_DEFAULT);
        requestRecord.addTag(Fields.REMOTE_HOST, NON_DEFAULT);
        requestRecord.addTag(Fields.RESPONSE_CONTENT_TYPE, NON_DEFAULT);

        assertRequestRecord(requestRecord).containsEntry(Fields.RESPONSE_TIME_MS, 0.0d)
                                          .containsEntry(Fields.LAYER, layer).containsEntry(Fields.REQUEST, NON_DEFAULT)
                                          .containsEntry(Fields.REMOTE_IP, NON_DEFAULT)
                                          .containsEntry(Fields.REMOTE_HOST, NON_DEFAULT)
                                          .containsEntry(Fields.PROTOCOL, NON_DEFAULT)
                                          .containsEntry(Fields.METHOD, NON_DEFAULT)
                                          .containsEntry(Fields.REMOTE_IP, NON_DEFAULT)
                                          .containsEntry(Fields.REMOTE_HOST, NON_DEFAULT)
                                          .containsEntry(Fields.RESPONSE_CONTENT_TYPE, NON_DEFAULT)
                                          .doesNotContainKey(Fields.REFERER).doesNotContainKey(Fields.X_FORWARDED_FOR)
                                          .doesNotContainKey(Fields.REMOTE_PORT);
    }

    @Test
    public void testContext() throws IOException {
        MDC.clear();
        String layer = "testContext";
        String reqId = "1-2-3-4";

        RequestRecord requestRecord = new RequestRecord(layer);
        requestRecord.addContextTag(Fields.REQUEST_ID, reqId);

        assertRequestRecord(requestRecord).doesNotContainKey(Fields.REQUEST_ID);
        assertThat(MDC.getCopyOfContextMap()).containsEntry(Fields.REQUEST_ID, reqId);
    }

    @Test
    public void testResponseTimeIn() throws IOException {
        MDC.clear();
        String layer = "testResponseTimeIn";
        RequestRecord requestRecord = new RequestRecord(layer);
        requestRecord.start();
        advanceRequestRecordClock(RESPONSE_DELAY);

        assertRequestRecord(requestRecord).containsEntry(Fields.LAYER, layer)
                                          .containsEntry(Fields.DIRECTION, Direction.IN.toString())
                                          .hasEntrySatisfying(Fields.RESPONSE_TIME_MS, v -> assertLatency(v).isEqualTo(
                                                  RESPONSE_DELAY.getNano() / 1_000_000d))
                                          .containsEntry(Fields.RESPONSE_SENT_AT,
                                                         Instant.EPOCH.plus(RESPONSE_DELAY).toString())
                                          .containsEntry(Fields.REQUEST_RECEIVED_AT, Instant.EPOCH.toString());
    }

    @Test
    public void testResponseTimeOut() throws IOException {
        MDC.clear();
        String layer = "testResponseTimeOut";
        RequestRecord requestRecord = new RequestRecord(layer, Direction.OUT);
        requestRecord.start();
        advanceRequestRecordClock(RESPONSE_DELAY);
        requestRecord.stop();

        assertRequestRecord(requestRecord).containsEntry(Fields.LAYER, layer)
                                          .containsEntry(Fields.DIRECTION, Direction.OUT.toString())
                                          .hasEntrySatisfying(Fields.RESPONSE_TIME_MS, v -> assertLatency(v).isEqualTo(
                                                  RESPONSE_DELAY.getNano() / 1_000_000d))
                                          .containsEntry(Fields.RESPONSE_RECEIVED_AT,
                                                         Instant.EPOCH.plus(RESPONSE_DELAY).toString())
                                          .containsEntry(Fields.REQUEST_SENT_AT, Instant.EPOCH.toString());

    }

    private Clock getRequestRecordClock() {
        return RequestRecord.ClockHolder.getInstance();
    }

    private void setRequestRecordClock(Clock clock) {
        RequestRecord.ClockHolder.instance = clock;
    }

    private void advanceRequestRecordClock(Duration duration) {
        Clock advancedClock = Clock.offset(getRequestRecordClock(), duration);
        setRequestRecordClock(advancedClock);
    }

}
