package com.sap.hcp.cf.logback.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.slf4j.MDC;
import org.slf4j.Marker;

public class CustomLoggingTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        String logLevel = MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        if (logLevel == null) {
            return FilterReply.NEUTRAL;
        }
        if (level.isGreaterOrEqual(Level.toLevel(logLevel))) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }
}
