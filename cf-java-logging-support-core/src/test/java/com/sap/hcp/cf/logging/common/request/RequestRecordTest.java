package com.sap.hcp.cf.logging.common.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.DoubleValue;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

public class RequestRecordTest {

    private static final Clock FIXED_CLOCK_EPOCH = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    private static final Duration RESPONSE_DELAY = Duration.ofMillis(150);

    private RequestRecord rrec;

    @Before
    public void resetRequestRecordClock() {
        setRequestRecordClock(FIXED_CLOCK_EPOCH);
    }

    @Test
    public void testDefaults() throws JSONObjectException, IOException {
        String layer = "testDefaults";
        rrec = new RequestRecord(layer);
        assertThat(getField(Fields.DIRECTION), is(Direction.IN.toString()));
        assertThat(getField(Fields.LAYER), is(layer));
        assertThat(getField(Fields.RESPONSE_SIZE_B), is("-1"));
        assertThat(getField(Fields.REQUEST_SIZE_B), is("-1"));
        assertThat(getField(Fields.REQUEST_RECEIVED_AT), not(nullValue()));
        assertThat(getField(Fields.REQUEST_RECEIVED_AT), not(nullValue()));
        assertThat(Double.valueOf(getField(Fields.RESPONSE_TIME_MS)), greaterThanOrEqualTo(0.0d));

        assertThat(getField(Fields.REQUEST), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.REMOTE_IP), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.REMOTE_HOST), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.PROTOCOL), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.METHOD), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.REMOTE_IP), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.REMOTE_HOST), is(Defaults.UNKNOWN));
        assertThat(getField(Fields.RESPONSE_CONTENT_TYPE), is(Defaults.UNKNOWN));

        assertThat(getField(Fields.REFERER), is(nullValue()));
        assertThat(getField(Fields.X_FORWARDED_FOR), is(nullValue()));
        assertThat(getField(Fields.REMOTE_PORT), is(nullValue()));

    }

    @Test
    public void testNonDefaults() throws JSONObjectException, IOException {
        String layer = "testNonDefaults";
        String NON_DEFAULT = "NON_DEFAULT";
        rrec = new RequestRecord(layer);
        rrec.addValue(Fields.RESPONSE_TIME_MS, new DoubleValue(0.0));
        rrec.addTag(Fields.REQUEST, NON_DEFAULT);
        rrec.addTag(Fields.REMOTE_IP, NON_DEFAULT);
        rrec.addTag(Fields.REMOTE_HOST, NON_DEFAULT);
        rrec.addTag(Fields.PROTOCOL, NON_DEFAULT);
        rrec.addTag(Fields.METHOD, NON_DEFAULT);
        rrec.addTag(Fields.REMOTE_IP, NON_DEFAULT);
        rrec.addTag(Fields.REMOTE_HOST, NON_DEFAULT);
        rrec.addTag(Fields.RESPONSE_CONTENT_TYPE, NON_DEFAULT);

        assertThat(getField(Fields.RESPONSE_TIME_MS), is("0.0"));
        assertThat(getField(Fields.LAYER), is(layer));

        assertThat(getField(Fields.REQUEST), is(NON_DEFAULT));
        assertThat(getField(Fields.REMOTE_IP), is(NON_DEFAULT));
        assertThat(getField(Fields.REMOTE_HOST), is(NON_DEFAULT));
        assertThat(getField(Fields.PROTOCOL), is(NON_DEFAULT));
        assertThat(getField(Fields.METHOD), is(NON_DEFAULT));
        assertThat(getField(Fields.REMOTE_IP), is(NON_DEFAULT));
        assertThat(getField(Fields.REMOTE_HOST), is(NON_DEFAULT));
        assertThat(getField(Fields.RESPONSE_CONTENT_TYPE), is(NON_DEFAULT));

        assertThat(getField(Fields.REFERER), is(nullValue()));
        assertThat(getField(Fields.X_FORWARDED_FOR), is(nullValue()));
        assertThat(getField(Fields.REMOTE_PORT), is(nullValue()));
    }

    @Test
    public void testContext() throws JSONObjectException, IOException {
        MDC.clear();
        String layer = "testContext";
        String reqId = "1-2-3-4";

        rrec = new RequestRecord(layer);
        rrec.addContextTag(Fields.REQUEST_ID, reqId);

        assertThat(getField(Fields.REQUEST_ID), is(nullValue()));
        assertThat(MDC.getCopyOfContextMap().get(Fields.REQUEST_ID), is(reqId));
    }

    @Test
    public void testResponseTimeIn() throws JSONObjectException, IOException {
        MDC.clear();
        String layer = "testResponseTimeIn";
        rrec = new RequestRecord(layer);
        rrec.start();
        advanceRequestRecordClock(RESPONSE_DELAY);
        rrec.stop();
        assertThat(getField(Fields.LAYER), is(layer));
        assertThat(getField(Fields.DIRECTION), is(Direction.IN.toString()));
        assertThat(Double.valueOf(getField(Fields.RESPONSE_TIME_MS)), is(RESPONSE_DELAY.getNano() / 1_000_000.0));
        assertThat(getField(Fields.RESPONSE_SENT_AT), is(equalTo(Instant.EPOCH.plus(RESPONSE_DELAY).toString())));
        assertThat(getField(Fields.REQUEST_RECEIVED_AT), is(equalTo(Instant.EPOCH.toString())));
    }

    @Test
    public void testResponseTimeOut() throws JSONObjectException, IOException {
        MDC.clear();
        String layer = "testResponseTimeOut";
        rrec = new RequestRecord(layer, Direction.OUT);
        rrec.start();
        advanceRequestRecordClock(RESPONSE_DELAY);
        rrec.stop();
        assertThat(getField(Fields.LAYER), is(layer));
        assertThat(getField(Fields.DIRECTION), is(Direction.OUT.toString()));
        assertThat(Double.valueOf(getField(Fields.RESPONSE_TIME_MS)), is(RESPONSE_DELAY.getNano() / 1_000_000.0));
        assertThat(getField(Fields.RESPONSE_RECEIVED_AT), not(nullValue()));
        assertThat(getField(Fields.REQUEST_SENT_AT), not(nullValue()));
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

    private String getField(String fieldName) throws JSONObjectException, IOException {
        Object field = JSON.std.mapFrom(rrec.toString()).get(fieldName);
        return field != null ? field.toString() : null;
    }

}
