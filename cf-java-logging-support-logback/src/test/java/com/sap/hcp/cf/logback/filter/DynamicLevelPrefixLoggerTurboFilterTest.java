package com.sap.hcp.cf.logback.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicLevelPrefixLoggerTurboFilterTest {

    private static final String KNOWN_PREFIX = "known.prefix";
    private static final String UNKNOWN_PREFIX = "unknown.prefix";

    private final LoggerContext loggerContext = new LoggerContext();
    private final TurboFilter filter = new DynamicLevelPrefixLoggerTurboFilter();

    @BeforeEach
    public void setUp() {
        MDC.clear();
        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "DEBUG");
        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, KNOWN_PREFIX);
        loggerContext.addTurboFilter(filter);
    }

    @Test
    public void acceptsOnKnownPackage() throws Exception {
        Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "acceptsOnKnownPackage");
        assertFilterDecisionForLevel(logger, Level.INFO).isEqualTo(FilterReply.ACCEPT);
    }

    private AbstractObjectAssert<?, FilterReply> assertFilterDecisionForLevel(Logger logger, Level level) {
        return assertThat(filter).extracting(f -> f.decide(null, logger, level, null, null, null));
    }

    @Test
    public void neutralOnUnknownPackage() throws Exception {
        Logger logger = loggerContext.getLogger(UNKNOWN_PREFIX + "neutralOnUnknownPackage");
        assertFilterDecisionForLevel(logger, Level.INFO).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    public void neutralOnLowerLevel() throws Exception {
        Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "neutralOnUnknownPackage");
        assertFilterDecisionForLevel(logger, Level.TRACE).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    public void neutralOnUnconfiguredLevelKey() throws Exception {
        MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "neutralOnUnknownPackage");
        assertFilterDecisionForLevel(logger, Level.INFO).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    public void neutralOnUnconfiguredPrefix() throws Exception {
        MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
        Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "neutralOnUnknownPackage");
        assertFilterDecisionForLevel(logger, Level.INFO).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    public void integratesIntoConfiguration() throws Exception {
        Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "integratesIntoConfiguration");
        ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.info("test-integration-message");

        assertThat(appender.list).extracting(ILoggingEvent::getMessage).containsExactly("test-integration-message");
        appender.stop();
    }
}
