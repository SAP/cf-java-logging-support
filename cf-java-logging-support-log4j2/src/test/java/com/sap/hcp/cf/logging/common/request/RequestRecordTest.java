package com.sap.hcp.cf.logging.common.request;

import com.sap.hcp.cf.logging.common.DoubleValue;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.Markers;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;
import org.assertj.core.api.AbstractDoubleAssert;
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

    private static AbstractDoubleAssert<?> assertLatency(Object v) {
        return assertThat(Double.valueOf(v.toString()));
    }

    @Test
    public void testDefaults(ConsoleOutput console) throws IOException {
        String layer = "testDefaults";
        RequestRecord requestRecord = new RequestRecord(layer);
        logger.info(Markers.REQUEST_MARKER, requestRecord.toString());

        assertLastEventFields(console)//
                                      .containsEntry(Fields.DIRECTION, Direction.IN.toString())
                                      .containsEntry(Fields.LAYER, layer).containsEntry(Fields.REQUEST_SIZE_B, -1)
                                      .containsEntry(Fields.REQUEST_SIZE_B, -1).containsKey(Fields.REQUEST_RECEIVED_AT)
                                      .hasEntrySatisfying(Fields.RESPONSE_TIME_MS,
                                                          v -> assertLatency(v).isGreaterThan(0))
                                      .doesNotContainKey(Fields.REFERER).doesNotContainKey(Fields.X_FORWARDED_FOR)
                                      .doesNotContainKey(Fields.REMOTE_PORT).containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testNonDefaults(ConsoleOutput console) throws IOException {
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

        logger.info(Markers.REQUEST_MARKER, requestRecord.toString());

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

        RequestRecord requestRecord = new RequestRecord(layer);
        requestRecord.addContextTag(Fields.REQUEST_ID, reqId);

        logger.info(Markers.REQUEST_MARKER, requestRecord.toString());

        assertLastEventFields(console).containsEntry(Fields.REQUEST_ID, reqId).containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testResponseTimeIn(ConsoleOutput console) throws IOException {
        MDC.clear();
        String layer = "testResponseTimeIn";
        RequestRecord requestRecord = new RequestRecord(layer);
        long start = requestRecord.start();
        doWait(150);
        long end = requestRecord.stop() + 1; // add 1 to cover for decimals in time recording

        logger.info(Markers.REQUEST_MARKER, requestRecord.toString());

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
        RequestRecord requestRecord = new RequestRecord(layer, Direction.OUT);
        long start = requestRecord.start();
        doWait(150);
        long end = requestRecord.stop() + 1; // add 1 to cover for decimals in time recording

        logger.info(Markers.REQUEST_MARKER, requestRecord.toString());

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
