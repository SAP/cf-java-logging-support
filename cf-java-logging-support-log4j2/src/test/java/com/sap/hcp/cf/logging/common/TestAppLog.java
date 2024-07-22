package com.sap.hcp.cf.logging.common;

import com.sap.hcp.cf.logging.common.customfields.CustomField;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.Assertions;
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

    private static AbstractLongAssert<?> assertTimestamp(Object v) {
        return Assertions.assertThat(Long.valueOf(v.toString()));
    }

    private static long now() {
        Instant now = Instant.now();
        return now.getEpochSecond() * 1_000_000_000L + now.getNano();
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
        assertLastEventFields(console).containsEntry(Fields.LOGGER, TestAppLog.class.getName())
                                      .containsEntry(Fields.THREAD, Thread.currentThread().getName())
                                      .containsKey(Fields.WRITTEN_TS).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);
    }

    @Test
    public void testCategories(ConsoleOutput console) {
        logMsg = "Running testCategories()";
        Marker cat0 = MarkerFactory.getMarker("cat0");

        LOGGER.info(cat0, logMsg);

        assertLastEventCategories(console).contains(cat0.getName());
        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventFields(console).containsKey(Fields.WRITTEN_TS).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);

        Marker cat1 = MarkerFactory.getMarker("cat1");
        cat1.add(cat0);

        LOGGER.info(cat1, logMsg);

        assertLastEventCategories(console).contains(cat1.getName(), cat0.getName());
        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventFields(console).containsKey(Fields.WRITTEN_TS).doesNotContainKey(Fields.COMPONENT_ID)
                                      .doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);
    }

    @Test
    public void testMDC(ConsoleOutput console) {
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put("testNumeric", "200");
        logMsg = "Running testMDC()";
        long beforeTS = now();
        LOGGER.info(logMsg);
        long afterTS = now();

        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventFields(console).hasEntrySatisfying(Fields.WRITTEN_TS,
                                                          v -> assertTimestamp(v).isBetween(beforeTS, afterTS))
                                      .doesNotContainKey(Fields.COMPONENT_ID).doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);
    }

    @Test
    public void testUnregisteredCustomField(ConsoleOutput console) {
        logMsg = "Running testUnregisteredCustomField()";
        long beforeTS = now();
        LOGGER.info(logMsg, CustomField.customField(SOME_KEY, SOME_VALUE));

        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventFields(console).containsEntry(SOME_KEY, SOME_VALUE) //
                                      .hasEntrySatisfying(Fields.WRITTEN_TS,
                                                          v -> assertTimestamp(v).isGreaterThanOrEqualTo(beforeTS))
                                      .doesNotContainKey(Fields.COMPONENT_ID).doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);
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
        assertLastEventFields(console).containsEntry(RETAINED_FIELD_KEY, SOME_OTHER_VALUE)
                                      .containsEntry(SOME_KEY, SOME_OTHER_VALUE) //
                                      .hasEntrySatisfying(Fields.WRITTEN_TS,
                                                          v -> assertTimestamp(v).isGreaterThanOrEqualTo(beforeTS))
                                      .doesNotContainKey(Fields.COMPONENT_ID).doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);
        assertLastEventCustomFields(console).hasSize(2) //
                                            .anySatisfy(e -> assertCustomField(e).hasKey(CUSTOM_FIELD_KEY)
                                                                                 .hasValue(SOME_OTHER_VALUE)
                                                                                 .hasIndex(CUSTOM_FIELD_INDEX))
                                            .anySatisfy(e -> assertCustomField(e).hasKey(RETAINED_FIELD_KEY)
                                                                                 .hasValue(SOME_OTHER_VALUE)
                                                                                 .hasIndex(RETAINED_FIELD_INDEX));
    }

    @Test
    public void testStacktrace(ConsoleOutput console) {
        try {
            Double.parseDouble(null);
        } catch (Exception ex) {
            logMsg = "Running testStacktrace()";
            LOGGER.error(logMsg, ex);
        }
        assertLastEventMessage(console).isEqualTo(logMsg);
        assertLastEventFields(console).containsKey(Fields.STACKTRACE).containsKey(Fields.WRITTEN_TS)
                                      .doesNotContainKey(Fields.COMPONENT_ID).doesNotContainKey(Fields.COMPONENT_NAME)
                                      .doesNotContainKey(Fields.COMPONENT_INSTANCE);

    }

    @Test
    public void testJSONMsg(ConsoleOutput console) {
        String jsonMsg = "{\"" + SOME_KEY + "\":\"" + SOME_VALUE + "\"}";
        LOGGER.info(jsonMsg);
        assertLastEventMessage(console).isEqualTo(jsonMsg);
    }
}
