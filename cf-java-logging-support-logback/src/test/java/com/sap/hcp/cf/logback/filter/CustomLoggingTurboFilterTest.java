package com.sap.hcp.cf.logback.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.slf4j.Marker;

import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;

@RunWith(MockitoJUnitRunner.class)
public class CustomLoggingTurboFilterTest {

    private CustomLoggingTurboFilter filter;

    @Mock
    private Marker marker;
    private Logger logger;
    private String format;
    @Mock
    private Object param;
    private Object[] params;
    @Mock
    private Throwable t;

    @Before
    public void setup() {
        filter = new CustomLoggingTurboFilter();
        params = new Object[] { param };
        format = "format";
    }

    @After
    public void teardown() {
        verifyNoMoreInteractions(marker, param, t);
        MDC.clear();
    }

    @Test
    public void testNeutralCondition() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.TRACE, format, params, t));
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.DEBUG, format, params, t));
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.INFO, format, params, t));
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.WARN, format, params, t));
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.ERROR, format, params, t));
    }

    @Test
    public void testAcceptCondition() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.ERROR, format, params, t));

        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "TRACE");

        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.TRACE, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.DEBUG, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.INFO, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.WARN, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.ERROR, format, params, t));
    }

    @Test
    public void testDenyCondition() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(marker, logger, Level.DEBUG, format, params, t));

        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "INFO");
        assertEquals(FilterReply.DENY, filter.decide(marker, logger, Level.TRACE, format, params, t));
        assertEquals(FilterReply.DENY, filter.decide(marker, logger, Level.DEBUG, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.INFO, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.WARN, format, params, t));
        assertEquals(FilterReply.ACCEPT, filter.decide(marker, logger, Level.ERROR, format, params, t));
    }
}
