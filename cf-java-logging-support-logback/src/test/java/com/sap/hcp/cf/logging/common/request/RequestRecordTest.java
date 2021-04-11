package com.sap.hcp.cf.logging.common.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.sap.hcp.cf.logging.common.AbstractTest;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.DoubleValue;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

public class RequestRecordTest extends AbstractTest {

    private final Logger logger = LoggerFactory.getLogger(RequestRecordTest.class);
    private RequestRecord rrec;

    @Test
    public void testDefaults() throws JSONObjectException, IOException {
        String layer = "testDefaults";
        rrec = new RequestRecord(layer);
        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertThat(getField(Fields.DIRECTION), is(Direction.IN.toString()));
        assertThat(getField(Fields.LAYER), is(layer));
        assertThat(getField(Fields.RESPONSE_SIZE_B), is("-1"));
        assertThat(getField(Fields.REQUEST_SIZE_B), is("-1"));
        assertThat(getField(Fields.REQUEST_RECEIVED_AT), not(nullValue()));
        assertThat(getField(Fields.REQUEST_RECEIVED_AT), not(nullValue()));
        assertThat(Double.valueOf(getField(Fields.RESPONSE_TIME_MS)), greaterThan(0.0d));

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
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));

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

        logger.info(Markers.REQUEST_MARKER, rrec.toString());

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
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
    }

    @Test
    public void testContext() throws JSONObjectException, IOException {
        MDC.clear();
        String layer = "testContext";
        String reqId = "1-2-3-4";

        rrec = new RequestRecord(layer);
        rrec.addContextTag(Fields.REQUEST_ID, reqId);

        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertThat(getField(Fields.REQUEST_ID), is(reqId));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
    }

    @Test
    public void testResponseTimeIn() throws JSONObjectException, IOException {
        MDC.clear();
        String layer = "testResponseTimeIn";
        rrec = new RequestRecord(layer);
        long start = rrec.start();
        doWait(150);
        long end = rrec.stop();

        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertThat(getField(Fields.LAYER), is(layer));
        assertThat(getField(Fields.DIRECTION), is(Direction.IN.toString()));
        assertThat(Double.valueOf(getField(Fields.RESPONSE_TIME_MS)).longValue(), lessThanOrEqualTo(Double.valueOf(end -
                                                                                                                   start)
                                                                                                          .longValue()));
        assertThat(getField(Fields.RESPONSE_SENT_AT), not(nullValue()));
        assertThat(getField(Fields.REQUEST_RECEIVED_AT), not(nullValue()));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
    }

    @Test
    public void testResponseTimeOut() throws JSONObjectException, IOException {
        MDC.clear();
        String layer = "testResponseTimeOut";
        rrec = new RequestRecord(layer, Direction.OUT);
        long start = rrec.start();
        doWait(150);
        long end = rrec.stop();

        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertThat(getField(Fields.LAYER), is(layer));
        assertThat(getField(Fields.DIRECTION), is(Direction.OUT.toString()));
        assertThat(Double.valueOf(getField(Fields.RESPONSE_TIME_MS)).longValue(), lessThanOrEqualTo(Double.valueOf(end -
                                                                                                                   start)
                                                                                                          .longValue()));
        assertThat(getField(Fields.RESPONSE_RECEIVED_AT), not(nullValue()));
        assertThat(getField(Fields.REQUEST_SENT_AT), not(nullValue()));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
    }

    private void doWait(long p) {
        try {
            Thread.sleep(p);
        } catch (Exception e) {

        }
    }
}
