package com.sap.hcp.cf.logback.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;

public class DynamicLevelPrefixLoggerTurboFilterTest {

	private static final String KNOWN_PREFIX = "known.prefix";
	private static final String UNKNOWN_PREFIX = "unknown.prefix";

	private LoggerContext loggerContext = new LoggerContext();
	private TurboFilter filter = new DynamicLevelPrefixLoggerTurboFilter();

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
		assertThat(filter.decide(null, logger, Level.INFO, null, null, null), is(FilterReply.ACCEPT));
	}

	@Test
	public void neutralOnUnknownPackage() throws Exception {
		Logger logger = loggerContext.getLogger(UNKNOWN_PREFIX + "neutralOnUnknownPackage");
		assertThat(filter.decide(null, logger, Level.INFO, null, null, null), is(FilterReply.NEUTRAL));
	}

	@Test
	public void neutralOnLowerLevel() throws Exception {
		Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "neutralOnUnknownPackage");
		assertThat(filter.decide(null, logger, Level.TRACE, null, null, null), is(FilterReply.NEUTRAL));
	}

	@Test
	public void neutralOnUnconfiguredLevelKey() throws Exception {
		MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
		Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "neutralOnUnknownPackage");
		assertThat(filter.decide(null, logger, Level.INFO, null, null, null), is(FilterReply.NEUTRAL));
	}

	@Test
	public void neutralOnUnconfiguredPrefix() throws Exception {
		MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
		Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "neutralOnUnknownPackage");
		assertThat(filter.decide(null, logger, Level.INFO, null, null, null), is(FilterReply.NEUTRAL));
	}

	@Test
	public void integratesIntoConfiguration() throws Exception {
		Logger logger = loggerContext.getLogger(KNOWN_PREFIX + "integratesIntoConfiguration");
		ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();
		appender.start();
		logger.addAppender(appender);
		logger.info("test-integration-message");
		assertThat(appender.list, hasSize(1));
		assertThat(appender.list.get(0).getMessage(), is(equalTo("test-integration-message")));
		appender.stop();
	}
}
