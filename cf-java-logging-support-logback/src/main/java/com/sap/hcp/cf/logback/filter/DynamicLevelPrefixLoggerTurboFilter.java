package com.sap.hcp.cf.logback.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.slf4j.MDC;
import org.slf4j.Marker;

public class DynamicLevelPrefixLoggerTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(final Marker marker, final Logger logger, final Level level, final String format,
                              final Object[] params, final Throwable t) {
        final String logLevel = MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        if (logLevel != null && level.isGreaterOrEqual(Level.toLevel(logLevel)) && checkPackages(logger)) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.NEUTRAL;
    }

    private boolean checkPackages(final Logger logger) {
        final String logLevelPackages = MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
        if (isNotBlank(logLevelPackages)) {
            for (String current: logLevelPackages.split(",")) {
                if (logger.getName().startsWith(current)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNotBlank(String string) {
        return string != null && !string.isBlank();
    }

}
