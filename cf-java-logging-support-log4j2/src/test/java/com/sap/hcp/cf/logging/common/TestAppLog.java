package com.sap.hcp.cf.logging.common;

import static com.sap.hcp.cf.logging.common.converter.CustomFieldMatchers.hasCustomField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.time.Instant;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.sap.hcp.cf.logging.common.customfields.CustomField;

public class TestAppLog extends AbstractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestAppLog.class);
    private String logMsg;

    @Test
    public void test() {
        logMsg = "Running test()";
        LOGGER.info(logMsg);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
    }

    @Test
    public void testCategorties() {
        logMsg = "Running testCategories()";
        Marker cat0 = MarkerFactory.getMarker("cat0");

        LOGGER.info(cat0, logMsg);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getList(Fields.CATEGORIES), contains(cat0.getName()));

        Marker cat1 = MarkerFactory.getMarker("cat1");
        cat1.add(cat0);

        LOGGER.info(cat1, logMsg);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getList(Fields.CATEGORIES), contains(cat1.getName(), cat0.getName()));
    }

    @Test
    public void testMDC() {
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put("testNumeric", "200");
        logMsg = "Running testMDC()";
		long beforeTS = now();
        LOGGER.info(logMsg);
		long afterTS = now();
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getField(Fields.WRITTEN_TS), greaterThanOrEqualTo(Long.toString(beforeTS)));
        assertThat(Long.toString(afterTS), greaterThanOrEqualTo(getField(Fields.WRITTEN_TS)));
    }

	@Test
	public void testUnregisteredCustomField() {
		logMsg = "Running testUnregisteredCustomField()";
		long beforeTS = now();
		LOGGER.info(logMsg, CustomField.customField(SOME_KEY, SOME_VALUE));
		assertThat(getMessage(), is(logMsg));
		assertThat(getField(SOME_KEY), is(SOME_VALUE));
        assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
		assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
		assertThat(getField(Fields.WRITTEN_TS), greaterThanOrEqualTo(Long.toString(beforeTS)));
		assertThat(Long.toString(beforeTS), lessThanOrEqualTo(getField(Fields.WRITTEN_TS)));
	}

	@Test
	public void testCustomFieldOverwritesMdc() throws Exception {
		MDC.put(CUSTOM_FIELD_KEY, SOME_VALUE);
		MDC.put(RETAINED_FIELD_KEY, SOME_VALUE);
		MDC.put(SOME_KEY, SOME_VALUE);
		logMsg = "Running testCustomFieldOverwritesMdc()";
		long beforeTS = now();
		LOGGER.info(logMsg, CustomField.customField(CUSTOM_FIELD_KEY, SOME_OTHER_VALUE),
				CustomField.customField(RETAINED_FIELD_KEY, SOME_OTHER_VALUE),
				CustomField.customField(SOME_KEY, SOME_OTHER_VALUE));
		assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
		assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
		assertThat(getField(Fields.WRITTEN_TS), greaterThanOrEqualTo(Long.toString(beforeTS)));
		assertThat(Long.toString(beforeTS), lessThanOrEqualTo(getField(Fields.WRITTEN_TS)));
		assertThat(getCustomField(CUSTOM_FIELD_KEY),
				hasCustomField(CUSTOM_FIELD_KEY, SOME_OTHER_VALUE, CUSTOM_FIELD_INDEX));
		assertThat(getCustomField(RETAINED_FIELD_KEY),
				hasCustomField(RETAINED_FIELD_KEY, SOME_OTHER_VALUE, RETAINED_FIELD_INDEX));
		assertThat(getField(RETAINED_FIELD_KEY), is(SOME_OTHER_VALUE));
		assertThat(getField(SOME_KEY), is(SOME_OTHER_VALUE));
	}

    @Test
    public void testStacktrace() {
        try {
            Double.parseDouble(null);
        } catch (Exception ex) {
            logMsg = "Running testStacktrace()";
            LOGGER.error(logMsg, ex);
            assertThat(getMessage(), is(logMsg));
            assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
            assertThat(getField(Fields.COMPONENT_NAME), is(nullValue()));
            assertThat(getField(Fields.COMPONENT_INSTANCE), is(nullValue()));
            assertThat(getField(Fields.STACKTRACE), is(notNullValue()));
            assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        }
    }

    @Test
    public void testJSONMsg() {
        String jsonMsg = "{\"" + SOME_KEY + "\":\"" + SOME_VALUE + "\"}";
        LOGGER.info(jsonMsg);
        assertThat(getMessage(), is(jsonMsg));
    }

	private static long now() {
		Instant now = Instant.now();
		return now.getEpochSecond() * 1_000_000_000L + now.getNano();
	}
}
