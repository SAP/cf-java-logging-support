package com.sap.hcp.cf.logging.common;

import com.sap.hcp.cf.logging.common.customfields.CustomField;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.*;

import java.time.Instant;

import static com.sap.hcp.cf.logging.common.LogMessageConstants.*;
import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.*;

@ExtendWith(ConsoleExtension.class)
public class TestAppLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestAppLog.class);
    private String logMsg;

    private static long now() {
        Instant now = Instant.now();
        return now.getEpochSecond() * 1_000_000_000L + now.getNano();
    }

    private static void assertDefaultComponent(ConsoleOutput console) {
        assertLastEventFields(console).containsEntry(Fields.COMPONENT_ID, "-");
        assertLastEventFields(console).containsEntry(Fields.COMPONENT_NAME, "-");
        assertLastEventFields(console).containsEntry(Fields.COMPONENT_INSTANCE, "0");
    }

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void test(ConsoleOutput console) {
        logMsg = "Running test()";

        LOGGER.info(logMsg);

        assertLastEventMessage(console).isEqualTo(logMsg);
        assertDefaultComponent(console);
        assertLastEventFields(console).containsKey(Fields.WRITTEN_TS);
        assertLastEventFields(console).containsEntry(Fields.LOGGER, TestAppLog.class.getName());
        assertLastEventFields(console).containsEntry(Fields.THREAD, Thread.currentThread().getName());
        assertLastEventCategories(console).isNotNull();
    }

    @Test
    public void testCategories(ConsoleOutput console) {
        logMsg = "Running testCategories()";
        Marker cat0 = MarkerFactory.getMarker("cat0");

        LOGGER.info(cat0, logMsg);
        assertLastEventMessage(console).isEqualTo(logMsg);
        assertDefaultComponent(console);
        assertLastEventFields(console).containsKey(Fields.WRITTEN_TS);
        assertLastEventCategories(console).contains(cat0.getName());

        Marker cat1 = MarkerFactory.getMarker("cat1");
        cat1.add(cat0);

        LOGGER.info(cat1, logMsg);
        assertLastEventMessage(console).isEqualTo(logMsg);
        assertDefaultComponent(console);
        assertLastEventFields(console).containsKey(Fields.WRITTEN_TS);
        assertLastEventCategories(console).contains(cat1.getName(), cat0.getName());
    }

    @Test
    public void testMDC(ConsoleOutput console) {
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put("testNumeric", "200");
        logMsg = "Running testMDC()";
        long beforeTS = now();
        LOGGER.info(logMsg);
        assertLastEventMessage(console).isEqualTo(logMsg);
        assertDefaultComponent(console);
        assertLastEventTimestamp(console).isGreaterThan(beforeTS);
        assertLastEventFields(console).containsEntry(SOME_KEY, SOME_VALUE);
        assertLastEventFields(console).containsEntry("testNumeric", "200");
    }

    @Test
    public void testUnregisteredCustomField(ConsoleOutput console) {
        logMsg = "Running testUnregisteredCustomField()";
        long beforeTS = now();

        LOGGER.info(logMsg, CustomField.customField(SOME_KEY, SOME_VALUE));

        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventFields(console).containsEntry(SOME_KEY, SOME_VALUE);
        assertDefaultComponent(console);
        assertLastEventTimestamp(console).isGreaterThan(beforeTS);
    }

    @Test
    public void testCustomFieldOverwritesMdc(ConsoleOutput console) throws Exception {
        MDC.put(CUSTOM_FIELD_KEY, SOME_VALUE);
        MDC.put(RETAINED_FIELD_KEY, SOME_VALUE);
        MDC.put(SOME_KEY, SOME_VALUE);
        logMsg = "Running testCustomFieldOverwritesMdc()";
        long beforeTS = now();

        LOGGER.info(logMsg, CustomField.customField(CUSTOM_FIELD_KEY, SOME_OTHER_VALUE),
                    CustomField.customField(RETAINED_FIELD_KEY, SOME_OTHER_VALUE),
                    CustomField.customField(SOME_KEY, SOME_OTHER_VALUE));

        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventCustomFields(console) //
                                             .anySatisfy(e -> assertCustomField(e).hasKey(CUSTOM_FIELD_KEY)
                                                                                  .hasValue(SOME_OTHER_VALUE)
                                                                                  .hasIndex(CUSTOM_FIELD_INDEX))
                                             .anySatisfy(e -> assertCustomField(e).hasKey(RETAINED_FIELD_KEY)
                                                                                  .hasValue(SOME_OTHER_VALUE)
                                                                                  .hasIndex(RETAINED_FIELD_INDEX));
        assertLastEventFields(console).containsEntry(RETAINED_FIELD_KEY, SOME_OTHER_VALUE);
        assertLastEventFields(console).containsEntry(SOME_KEY, SOME_OTHER_VALUE);
        assertDefaultComponent(console);
        assertLastEventTimestamp(console).isGreaterThan(beforeTS);
    }

    @Test
    public void testStacktrace(ConsoleOutput console) {
        logMsg = "Running testStacktrace()";
        Exception testEx = new Exception("Test-case for Stacktraces.");

        LOGGER.error(logMsg, testEx);

        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventStacktrace(console).first().contains("java.lang.Exception", "Test-case for Stacktraces.");
        assertDefaultComponent(console);
        assertLastEventFields(console).containsKey(Fields.WRITTEN_TS);
    }

    @Test
    public void testJSONMsg(ConsoleOutput console) {
        String jsonMsg = "{\"" + SOME_KEY + "\":\"" + SOME_VALUE + "\"}";
        LOGGER.info(jsonMsg);
        assertLastEventMessage(console).isEqualTo(jsonMsg);
    }

}
