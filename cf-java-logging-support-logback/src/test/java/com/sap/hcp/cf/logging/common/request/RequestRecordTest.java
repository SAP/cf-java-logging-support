package com.sap.hcp.cf.logging.common.request;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.DoubleValue;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;
import org.assertj.core.api.AbstractDoubleAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;

import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.assertLastEventFields;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ConsoleExtension.class)
public class RequestRecordTest {

    private final Logger logger = LoggerFactory.getLogger(RequestRecordTest.class);
    private RequestRecord rrec;

    private static AbstractDoubleAssert<?> assertLatency(Object v) {
        return assertThat(Double.valueOf(v.toString()));
    }

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void testDefaults(ConsoleOutput console) throws IOException {
        String layer = "testDefaults";
        rrec = new RequestRecord(layer);
        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertLastEventFields(console)//
                                      .containsEntry(Fields.DIRECTION, Direction.IN.toString())
                                      .containsEntry(Fields.LAYER, layer).containsEntry(Fields.REQUEST_SIZE_B, -1)
                                      .containsEntry(Fields.REQUEST_SIZE_B, -1).containsKey(Fields.REQUEST_RECEIVED_AT)
                                      .hasEntrySatisfying(Fields.RESPONSE_TIME_MS,
                                                          v -> assertLatency(v).isGreaterThan(0))
                                      .containsEntry(Fields.REQUEST, Defaults.UNKNOWN)
                                      .containsEntry(Fields.REMOTE_IP, Defaults.UNKNOWN)
                                      .containsEntry(Fields.REMOTE_HOST, Defaults.UNKNOWN)
                                      .containsEntry(Fields.PROTOCOL, Defaults.UNKNOWN)
                                      .containsEntry(Fields.METHOD, Defaults.UNKNOWN)
                                      .containsEntry(Fields.REMOTE_IP, Defaults.UNKNOWN)
                                      .containsEntry(Fields.RESPONSE_CONTENT_TYPE, Defaults.UNKNOWN)
                                      .containsEntry(Fields.REMOTE_HOST, Defaults.UNKNOWN)
                                      .doesNotContainKey(Fields.REFERER).doesNotContainKey(Fields.X_FORWARDED_FOR)
                                      .doesNotContainKey(Fields.REMOTE_PORT).containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testNonDefaults(ConsoleOutput console) throws IOException {
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

        assertLastEventFields(console)//
                                      .containsEntry(Fields.RESPONSE_TIME_MS, 0.0d).containsEntry(Fields.LAYER, layer)
                                      .containsEntry(Fields.REQUEST, NON_DEFAULT)
                                      .containsEntry(Fields.REMOTE_IP, NON_DEFAULT)
                                      .containsEntry(Fields.REMOTE_HOST, NON_DEFAULT)
                                      .containsEntry(Fields.PROTOCOL, NON_DEFAULT)
                                      .containsEntry(Fields.METHOD, NON_DEFAULT)
                                      .containsEntry(Fields.REMOTE_IP, NON_DEFAULT)
                                      .containsEntry(Fields.REMOTE_HOST, NON_DEFAULT)
                                      .containsEntry(Fields.RESPONSE_CONTENT_TYPE, NON_DEFAULT)
                                      .doesNotContainKey(Fields.REFERER).doesNotContainKey(Fields.X_FORWARDED_FOR)
                                      .doesNotContainKey(Fields.REMOTE_PORT).containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testContext(ConsoleOutput console) throws IOException {
        MDC.clear();
        String layer = "testContext";
        String reqId = "1-2-3-4";

        rrec = new RequestRecord(layer);
        rrec.addContextTag(Fields.REQUEST_ID, reqId);

        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertLastEventFields(console).containsEntry(Fields.REQUEST_ID, reqId).containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testResponseTimeIn(ConsoleOutput console) throws IOException {
        MDC.clear();
        String layer = "testResponseTimeIn";
        rrec = new RequestRecord(layer);
        long start = rrec.start();
        doWait(150);
        long end = rrec.stop() + 1; // add 1 to cover for decimals in time recording
        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertLastEventFields(console) //
                                       .containsEntry(Fields.LAYER, layer)
                                       .containsEntry(Fields.DIRECTION, Direction.IN.toString())
                                       .hasEntrySatisfying(Fields.RESPONSE_TIME_MS,
                                                           v -> assertLatency(v).isLessThanOrEqualTo(end - start))
                                       .containsKey(Fields.RESPONSE_SENT_AT).containsKey(Fields.REQUEST_RECEIVED_AT)
                                       .containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testResponseTimeOut(ConsoleOutput console) throws IOException {
        MDC.clear();
        String layer = "testResponseTimeOut";
        rrec = new RequestRecord(layer, Direction.OUT);
        long start = rrec.start();
        doWait(150);
        long end = rrec.stop() + 1; // add 1 to cover for decimals in time recording

        logger.info(Markers.REQUEST_MARKER, rrec.toString());

        assertLastEventFields(console) //
                                       .containsEntry(Fields.LAYER, layer)
                                       .containsEntry(Fields.DIRECTION, Direction.OUT.toString())
                                       .hasEntrySatisfying(Fields.RESPONSE_TIME_MS,
                                                           v -> assertLatency(v).isLessThanOrEqualTo(end - start))
                                       .containsKey(Fields.RESPONSE_RECEIVED_AT).containsKey(Fields.REQUEST_SENT_AT)
                                       .containsKey(Fields.WRITTEN_TS);
    }

    private void doWait(long p) {
        try {
            Thread.sleep(p);
        } catch (Exception e) {

        }
    }
}
