package com.sap.hcp.cf.log4j2.converter;

import static com.sap.hcp.cf.logging.common.customfields.CustomField.customField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.junit.Test;
import org.slf4j.MDC;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

public class ContextPropsConverterTest extends AbstractConverterTest {

    private static final String[] EXCLUDE_SOME_KEY = new String[] { Boolean.FALSE.toString(), SOME_KEY };
    private static final String[] NO_EXCLUSIONS = new String[] { Boolean.FALSE.toString() };

	@Test
    public void testEmpty() throws JSONObjectException, IOException {
		ContextPropsConverter cpc = new ContextPropsConverter(NO_EXCLUSIONS);
        MDC.clear();
        assertThat(mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS))).size(), is(0));
    }

    @Test
    public void testEmptyWithDefaults() throws JSONObjectException, IOException {
        ContextPropsConverter cpc = new ContextPropsConverter(new String[] { Boolean.TRUE.toString() });
        MDC.clear();
        assertThat(mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS))), is(mdcMap()));
    }

    @Test
    public void testSingleArg() throws JSONObjectException, IOException {
		ContextPropsConverter cpc = new ContextPropsConverter(NO_EXCLUSIONS);
        MDC.clear();
        MDC.put(SOME_KEY, SOME_VALUE);
        Map<String, Object> resultMap = mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS)));
        assertThat(resultMap, hasEntry(SOME_KEY, SOME_VALUE));
        assertThat(resultMap.size(), is(1));
    }

    @Test
    public void testTwoArgs() throws JSONObjectException, IOException {
		ContextPropsConverter cpc = new ContextPropsConverter(NO_EXCLUSIONS);
        MDC.clear();
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put(SOME_OTHER_KEY, SOME_OTHER_VALUE);
        Map<String, Object> resultMap = mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS)));
        assertThat(resultMap, hasEntry(SOME_KEY, SOME_VALUE));
        assertThat(resultMap, hasEntry(SOME_OTHER_KEY, SOME_OTHER_VALUE));
        assertThat(resultMap.size(), is(2));

    }

    @Test
    public void testStrangeArgs() throws JSONObjectException, IOException {
		ContextPropsConverter cpc = new ContextPropsConverter(NO_EXCLUSIONS);
        MDC.clear();
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put(STRANGE_SEQ, STRANGE_SEQ);
        Map<String, Object> resultMap = mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS)));
        assertThat(resultMap, hasEntry(SOME_KEY, SOME_VALUE));
        assertThat(resultMap, hasEntry(STRANGE_SEQ, STRANGE_SEQ));
        assertThat(resultMap.size(), is(2));
    }

    @Test
    public void testExclusion() throws JSONObjectException, IOException {
		ContextPropsConverter cpc = new ContextPropsConverter(EXCLUDE_SOME_KEY);
        MDC.clear();
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put(SOME_OTHER_KEY, SOME_OTHER_VALUE);
        Map<String, Object> resultMap = mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS)));
        assertThat(resultMap, hasEntry(SOME_OTHER_KEY, SOME_OTHER_VALUE));
        assertThat(resultMap.size(), is(1));
    }

    @Test
    public void testExclusionStrangeSeq() throws JSONObjectException, IOException {
        String[] exclusions = new String[] { Boolean.FALSE.toString(), STRANGE_SEQ };
        ContextPropsConverter cpc = new ContextPropsConverter(exclusions);
        MDC.clear();
        MDC.put(SOME_KEY, SOME_VALUE);
        MDC.put(STRANGE_SEQ, STRANGE_SEQ);
        Map<String, Object> resultMap = mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, NO_ARGS)));
        assertThat(resultMap, hasEntry(SOME_KEY, SOME_VALUE));
        assertThat(resultMap.size(), is(1));
    }

	@Test
	public void testUnregisteredCustomField() throws Exception {
		ContextPropsConverter cpc = new ContextPropsConverter(NO_EXCLUSIONS);
		assertThat(mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, customField(SOME_KEY, SOME_VALUE)))),
				hasEntry(SOME_KEY, SOME_VALUE));
	}

	@Test
	public void testRegisteredCustomField() throws Exception {
		ContextPropsConverter cpc = new ContextPropsConverter(EXCLUDE_SOME_KEY);
		assertThat(mapFrom(format(cpc, makeEvent(TEST_MSG_NO_ARGS, customField(SOME_KEY, SOME_VALUE)))),
				not(hasEntry(SOME_KEY, SOME_VALUE)));
	}

    @Override
    protected String format(LogEventPatternConverter cpc, LogEvent event) {
        String converted = super.format(cpc, event);
        return converted.length() > 0 ? converted.substring(0, converted.length() - 1) : converted;
    }

}
