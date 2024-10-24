package com.sap.hcp.cf.logging.common;

import static com.sap.hcp.cf.logging.common.converter.CustomFieldMatchers.hasCustomField;
import static com.sap.hcp.cf.logging.common.customfields.CustomField.customField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TestCustomFields extends AbstractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCustomFields.class);

    @Test
    public void testLogMessage() {
        LOGGER.info(TEST_MESSAGE);
        assertThat(getMessage(), is(TEST_MESSAGE));
    }

    @Test
    public void testLogMessageWithCustomField() throws Exception {
        LOGGER.info(TEST_MESSAGE, customField(CUSTOM_FIELD_KEY, SOME_VALUE));

        assertThat(getMessage(), is(TEST_MESSAGE));
        assertThat(getCustomField(CUSTOM_FIELD_KEY), hasCustomField(CUSTOM_FIELD_KEY, SOME_VALUE, CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testCustomFieldWithoutRegistration() throws Exception {
        LOGGER.info(TEST_MESSAGE, customField("ungregistered", SOME_VALUE));

        assertThat(getField("ungregistered"), is(SOME_VALUE));
        assertThat(getCustomField("unregistered"), is(nullValue()));
    }

    @Test
    public void testCustomFieldAsPartOfMessage() throws Exception {
        String messageWithPattern = TEST_MESSAGE + " {}";
        String messageWithKeyValue = TEST_MESSAGE + " " + CUSTOM_FIELD_KEY + "=" + SOME_VALUE;

        LOGGER.info(messageWithPattern, customField(CUSTOM_FIELD_KEY, SOME_VALUE));

        assertThat(getMessage(), is(messageWithKeyValue));
        assertThat(getCustomField(CUSTOM_FIELD_KEY), hasCustomField(CUSTOM_FIELD_KEY, SOME_VALUE, CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testEscape() throws Exception {
        String messageWithPattern = TEST_MESSAGE + " {}";
        String messageWithKeyValue = TEST_MESSAGE + " " + CUSTOM_FIELD_KEY + "=" + HACK_ATTEMPT;

        LOGGER.info(messageWithPattern, customField(CUSTOM_FIELD_KEY, HACK_ATTEMPT));

        assertThat(getMessage(), is(messageWithKeyValue));
        assertThat(getCustomField(CUSTOM_FIELD_KEY), hasCustomField(CUSTOM_FIELD_KEY, HACK_ATTEMPT,
                                                                    CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testNullKey() {
        assertThrows(IllegalArgumentException.class, () ->
            customField(null, SOME_VALUE));
    }

    @Test
    public void testNullValue() throws Exception {
        LOGGER.info(TEST_MESSAGE, customField(CUSTOM_FIELD_KEY, null));

        assertThat(getMessage(), is(TEST_MESSAGE));
        assertThat(getCustomField(CUSTOM_FIELD_KEY), is(nullValue()));
    }

    @Test
    public void testLogMessageWithTwoCustomFields() throws Exception {
        LOGGER.info(TEST_MESSAGE, customField(TEST_FIELD_KEY, SOME_VALUE), customField(CUSTOM_FIELD_KEY,
                                                                                       SOME_OTHER_VALUE));

        assertThat(getMessage(), is(TEST_MESSAGE));

        assertThat(getCustomField(TEST_FIELD_KEY), hasCustomField(TEST_FIELD_KEY, SOME_VALUE, TEST_FIELD_INDEX));
        assertThat(getCustomField(CUSTOM_FIELD_KEY), hasCustomField(CUSTOM_FIELD_KEY, SOME_OTHER_VALUE,
                                                                    CUSTOM_FIELD_INDEX));
    }

    @Test
    public void testCustomFieldFromMdcWithoutRetention() throws Exception {
        MDC.put(TEST_FIELD_KEY, SOME_VALUE);

        LOGGER.info(TEST_MESSAGE);

        assertThat(getCustomField(TEST_FIELD_KEY), hasCustomField(TEST_FIELD_KEY, SOME_VALUE, TEST_FIELD_INDEX));
        assertThat(getField(TEST_FIELD_KEY), is(nullValue()));
    }

    @Test
    public void testCustomFieldFromMdcWithRetention() throws Exception {
        MDC.put(RETAINED_FIELD_KEY, SOME_VALUE);

        LOGGER.info(TEST_MESSAGE);

        assertThat(getCustomField(RETAINED_FIELD_KEY), hasCustomField(RETAINED_FIELD_KEY, SOME_VALUE,
                                                                      RETAINED_FIELD_INDEX));
        assertThat(getField(RETAINED_FIELD_KEY), is(SOME_VALUE));
    }
}
