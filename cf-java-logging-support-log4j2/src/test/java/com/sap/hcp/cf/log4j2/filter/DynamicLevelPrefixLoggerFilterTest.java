package com.sap.hcp.cf.log4j2.filter;

import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.CountingNoOpAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class DynamicLevelPrefixLoggerFilterTest {

    private static final String KNOWN_PREFIX = "known.prefix";
    private static final String UNKNOWN_PREFIX = "unknown.prefix";

    private final JdkMapAdapterStringMap contextData = new JdkMapAdapterStringMap(
            Map.ofEntries(entry(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "DEBUG"),
                          entry(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, KNOWN_PREFIX)));
    private final DynamicLevelPrefixLoggerFilter filter = new DynamicLevelPrefixLoggerFilter();

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void acceptsOnKnownPackage() throws Exception {
        Log4jLogEvent event = Log4jLogEvent.newBuilder().setLoggerFqcn(KNOWN_PREFIX + "acceptsOnKnownPackage")
                                           .setContextData(contextData).setLevel(Level.INFO).build();
        assertThat(filter.filter(event)).isEqualTo(Result.ACCEPT);
    }

    @Test
    public void neutralOnUnknownPackage() throws Exception {
        Log4jLogEvent event = Log4jLogEvent.newBuilder().setLoggerFqcn(UNKNOWN_PREFIX + "neutralOnUnknownPackage")
                                           .setContextData(contextData).setLevel(Level.INFO).build();
        assertThat(filter.filter(event)).isEqualTo(Result.NEUTRAL);
    }

    @Test
    public void neutralOnLowerLevel() throws Exception {
        Log4jLogEvent event = Log4jLogEvent.newBuilder().setLoggerFqcn(KNOWN_PREFIX + "neutralOnUnknownPackage")
                                           .setContextData(contextData).setLevel(Level.TRACE).build();
        assertThat(filter.filter(event)).isEqualTo(Result.NEUTRAL);
    }

    @Test
    public void neutralOnUnconfiguredLevelKey() throws Exception {
        HashMap<String, String> customMDC = new HashMap<>();
        customMDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, KNOWN_PREFIX);
        JdkMapAdapterStringMap missingLevelKey = new JdkMapAdapterStringMap(customMDC);
        Log4jLogEvent event = Log4jLogEvent.newBuilder().setLoggerFqcn(KNOWN_PREFIX + "neutralOnUnknownPackage")
                                           .setContextData(missingLevelKey).setLevel(Level.INFO).build();
        assertThat(filter.filter(event)).isEqualTo(Result.NEUTRAL);
    }

    @Test
    public void neutralOnUnconfiguredPrefix() throws Exception {
        HashMap<String, String> customMDC = new HashMap<>();
        customMDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, KNOWN_PREFIX);
        JdkMapAdapterStringMap missingPrefixes = new JdkMapAdapterStringMap(customMDC);
        Log4jLogEvent event = Log4jLogEvent.newBuilder().setLoggerFqcn(KNOWN_PREFIX + "neutralOnUnknownPackage")
                                           .setContextData(missingPrefixes).setLevel(Level.INFO).build();
        assertThat(filter.filter(event)).isEqualTo(Result.NEUTRAL);
    }

    @Test
    public void integratesIntoConfiguration() throws Exception {

        LoggerContext loggerContext = new LoggerContext("integratesIntoConfiguration");
        URL configLocation = getClass().getResource("log4j2-test.xml");
        ConfigurationSource configurationSource = ConfigurationSource.fromUri(configLocation.toURI());
        XmlConfiguration configuration = new XmlConfiguration(loggerContext, configurationSource);
        loggerContext.start(configuration);
        org.apache.logging.log4j.core.Logger logger =
                loggerContext.getLogger(KNOWN_PREFIX + "integratesIntoConfiguration");
        CountingNoOpAppender appender = new CountingNoOpAppender("integratesIntoConfiguration", null);
        logger.addAppender(appender);
        logger.debug("test-integration-message-at-debug");
        assertThat(appender.getCount()).isEqualTo(0L);
        logger.info("test-integration-message-at-info");
        assertThat(appender.getCount()).isEqualTo(1L);
        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, "DEBUG");
        MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, KNOWN_PREFIX);
        logger.debug("test-integration-message-at-debug");
        assertThat(appender.getCount()).isEqualTo(2L);
        loggerContext.close();
    }
}
