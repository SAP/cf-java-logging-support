package com.sap.hcp.cf.logback.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomLoggingTurboFilterTest {

    private static final String FORMAT = "format";
    private static final Marker MARKER = MarkerFactory.getMarker("test-marker");
    private static final Object[] PARAMS = new Object[] { new Object() };
    private static final Throwable THROWN = new Throwable();

    private final CustomLoggingTurboFilter filter = new CustomLoggingTurboFilter();

    private final Logger logger = Mockito.mock(Logger.class);

    @BeforeEach
    @AfterEach
    public void clear() {
        MDC.clear();
    }

    @Test
    public void testNeutralCondition() {
        assertThat(filter.decide(MARKER, logger, Level.TRACE, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(MARKER, logger, Level.DEBUG, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(MARKER, logger, Level.INFO, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(MARKER, logger, Level.WARN, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(MARKER, logger, Level.ERROR, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    public void testAcceptCondition() {
        assertThat(filter.decide(MARKER, logger, Level.ERROR, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);

        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "TRACE");

        assertThat(filter.decide(MARKER, logger, Level.TRACE, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
        assertThat(filter.decide(MARKER, logger, Level.DEBUG, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
        assertThat(filter.decide(MARKER, logger, Level.INFO, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
        assertThat(filter.decide(MARKER, logger, Level.WARN, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
        assertThat(filter.decide(MARKER, logger, Level.ERROR, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    public void testDenyCondition() {
        assertThat(filter.decide(MARKER, logger, Level.DEBUG, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.NEUTRAL);

        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "INFO");

        assertThat(filter.decide(MARKER, logger, Level.TRACE, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(MARKER, logger, Level.DEBUG, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(MARKER, logger, Level.INFO, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
        assertThat(filter.decide(MARKER, logger, Level.WARN, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
        assertThat(filter.decide(MARKER, logger, Level.ERROR, FORMAT, PARAMS, THROWN)).isEqualTo(FilterReply.ACCEPT);
    }
}
