package com.sap.hcp.cf.logging.common;

import static com.sap.hcp.cf.logging.common.converter.CustomFieldMatchers.hasCustomField;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.time.Instant;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
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
        assertThat(getField(Fields.COMPONENT_ID), is("-"));
        assertThat(getField(Fields.COMPONENT_NAME), is("-"));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getField(Fields.LOGGER), is(TestAppLog.class.getName()));
        assertThat(getField(Fields.THREAD), is(Thread.currentThread().getName()));
        assertThat(getField(Fields.CATEGORIES), is(notNullValue()));
    }

    @Test
    public void testCategories() {
        logMsg = "Running testCategories()";
        Marker cat0 = MarkerFactory.getMarker("cat0");

        LOGGER.info(cat0, logMsg);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is("-"));
        assertThat(getField(Fields.COMPONENT_NAME), is("-"));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getList(Fields.CATEGORIES), contains(cat0.getName()));

        Marker cat1 = MarkerFactory.getMarker("cat1");
        cat1.add(cat0);

        LOGGER.info(cat1, logMsg);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is("-"));
        assertThat(getField(Fields.COMPONENT_NAME), is("-"));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getList(Fields.CATEGORIES), Matchers.containsInAnyOrder(cat1.getName(), cat0.getName()));
    }

    @Test
    public void testMDC() {
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put("testNumeric", "200");
        logMsg = "Running testMDC()";
        long beforeTS = now();
        LOGGER.info(logMsg);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is("-"));
        assertThat(getField(Fields.COMPONENT_NAME), is("-"));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
        assertThat(getField(Fields.WRITTEN_TS), greaterThanOrEqualTo(Long.toString(beforeTS)));
        assertThat(Long.toString(beforeTS), lessThanOrEqualTo(getField(Fields.WRITTEN_TS)));
		assertThat(getField(SOME_KEY), is(SOME_VALUE));
		assertThat(getField("testNumeric"), is("200"));
    }

    @Test
	public void testUnregisteredCustomField() {
		logMsg = "Running testUnregisteredCustomField()";
		long beforeTS = now();
		LOGGER.info(logMsg, CustomField.customField(SOME_KEY, SOME_VALUE));
		assertThat(getMessage(), is(logMsg));
		assertThat(getField(SOME_KEY), is(SOME_VALUE));
		assertThat(getField(Fields.COMPONENT_ID), is("-"));
		assertThat(getField(Fields.COMPONENT_NAME), is("-"));
		assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
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
		assertThat(getField(Fields.COMPONENT_ID), is("-"));
		assertThat(getField(Fields.COMPONENT_NAME), is("-"));
		assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
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
        logMsg = "Running testStacktrace()";
        Exception testEx = new Exception("Test-case for Stacktraces.");
        LOGGER.error(logMsg, testEx);
        assertThat(getMessage(), is(logMsg));
        assertThat(getField(Fields.COMPONENT_ID), is("-"));
        assertThat(getField(Fields.COMPONENT_NAME), is("-"));
        assertThat(getField(Fields.COMPONENT_INSTANCE), is("0"));
        assertThat(getField(Fields.STACKTRACE), stringContainsInOrder(asList("java.lang.Exception", "Test-case for Stacktraces.")));
        assertThat(getField(Fields.WRITTEN_TS), is(notNullValue()));
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
