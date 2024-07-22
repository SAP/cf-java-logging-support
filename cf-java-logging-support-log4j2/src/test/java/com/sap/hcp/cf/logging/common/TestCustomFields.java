package com.sap.hcp.cf.logging.common;

import com.sap.hcp.cf.logging.common.helper.ConsoleAssertions;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.sap.hcp.cf.logging.common.LogMessageConstants.*;
import static com.sap.hcp.cf.logging.common.customfields.CustomField.customField;
import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(ConsoleExtension.class)
public class TestCustomFields {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCustomFields.class);

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void testLogMessage(ConsoleOutput console) {
        LOGGER.info(TEST_MESSAGE);

        assertLastEventMessage(console).isEqualTo(TEST_MESSAGE);
    }

    @Test
    public void testLogMessageWithCustomField(ConsoleOutput console) throws Exception {
        LOGGER.info(TEST_MESSAGE, customField(CUSTOM_FIELD_KEY, SOME_VALUE));

        assertLastEventMessage(console).isEqualTo(TEST_MESSAGE);
        assertLastEventCustomFields(console).hasSize(1) //
                                            .anySatisfy(
                                                    e -> ConsoleAssertions.assertCustomField(e).hasKey(CUSTOM_FIELD_KEY)
                                                                          .hasValue(SOME_VALUE)
                                                                          .hasIndex(CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testCustomFieldWithoutRegistration(ConsoleOutput console) throws Exception {
        LOGGER.info(TEST_MESSAGE, customField("unregistered", SOME_VALUE));

        assertLastEventFields(console).containsEntry("unregistered", SOME_VALUE);
        assertLastEventCustomFields(console).isEmpty();
    }

    @Test
    public void testCustomFieldAsPartOfMessage(ConsoleOutput console) throws Exception {
        String messageWithPattern = TEST_MESSAGE + " {}";
        String messageWithKeyValue = TEST_MESSAGE + " " + CUSTOM_FIELD_KEY + "=" + SOME_VALUE;

        LOGGER.info(messageWithPattern, customField(CUSTOM_FIELD_KEY, SOME_VALUE));

        assertLastEventMessage(console).isEqualTo(messageWithKeyValue);
        assertLastEventCustomFields(console).hasSize(1) //
                                            .anySatisfy(
                                                    e -> ConsoleAssertions.assertCustomField(e).hasKey(CUSTOM_FIELD_KEY)
                                                                          .hasValue(SOME_VALUE)
                                                                          .hasIndex(CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testEscape(ConsoleOutput console) throws Exception {
        String messageWithPattern = TEST_MESSAGE + " {}";
        String messageWithKeyValue = TEST_MESSAGE + " " + CUSTOM_FIELD_KEY + "=" + HACK_ATTEMPT;

        LOGGER.info(messageWithPattern, customField(CUSTOM_FIELD_KEY, HACK_ATTEMPT));

        assertLastEventMessage(console).isEqualTo(messageWithKeyValue);
        assertLastEventCustomFields(console).hasSize(1) //
                                            .anySatisfy(
                                                    e -> ConsoleAssertions.assertCustomField(e).hasKey(CUSTOM_FIELD_KEY)
                                                                          .hasValue(HACK_ATTEMPT)
                                                                          .hasIndex(CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testNullKey() {
        assertThrows(IllegalArgumentException.class, () -> customField(null, SOME_VALUE));

    }

    @Test
    public void testNullValue(ConsoleOutput console) throws Exception {
        LOGGER.info(TEST_MESSAGE, customField(CUSTOM_FIELD_KEY, null));

        assertLastEventMessage(console).isEqualTo(TEST_MESSAGE);
        assertLastEventCustomFields(console).isEmpty();
    }

    @Test
    public void testLogMessageWithTwoCustomFields(ConsoleOutput console) throws Exception {
        LOGGER.info(TEST_MESSAGE, customField(TEST_FIELD_KEY, SOME_VALUE),
                    customField(CUSTOM_FIELD_KEY, SOME_OTHER_VALUE));

        assertLastEventMessage(console).isEqualTo(TEST_MESSAGE);
        assertLastEventCustomFields(console).hasSize(2) //
                                            .anySatisfy(
                                                    e -> ConsoleAssertions.assertCustomField(e).hasKey(TEST_FIELD_KEY)
                                                                          .hasValue(SOME_VALUE)
                                                                          .hasIndex(TEST_FIELD_INDEX)).anySatisfy(
                                                    e -> ConsoleAssertions.assertCustomField(e).hasKey(CUSTOM_FIELD_KEY).hasValue(SOME_OTHER_VALUE)
                                                                          .hasIndex(CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testCustomFieldFromMdcWithoutRetention(ConsoleOutput console) throws Exception {
        MDC.put(TEST_FIELD_KEY, SOME_VALUE);

        LOGGER.info(TEST_MESSAGE);

        assertLastEventFields(console).doesNotContainKey(TEST_FIELD_KEY);
        assertLastEventCustomFields(console).hasSize(1) //
                                            .anySatisfy(
                                                    e -> ConsoleAssertions.assertCustomField(e).hasKey(TEST_FIELD_KEY)
                                                                          .hasValue(SOME_VALUE)
                                                                          .hasIndex(TEST_FIELD_INDEX));
    }

    @Test
    public void testCustomFieldFromMdcWithRetention(ConsoleOutput console) throws Exception {
        MDC.put(RETAINED_FIELD_KEY, SOME_VALUE);

        LOGGER.info(TEST_MESSAGE);

        assertLastEventFields(console).containsEntry(RETAINED_FIELD_KEY, SOME_VALUE);
        assertLastEventCustomFields(console).hasSize(1) //
                                            .anySatisfy(e -> ConsoleAssertions.assertCustomField(e)
                                                                              .hasKey(RETAINED_FIELD_KEY)
                                                                              .hasValue(SOME_VALUE)
                                                                              .hasIndex(RETAINED_FIELD_INDEX));
    }
}
