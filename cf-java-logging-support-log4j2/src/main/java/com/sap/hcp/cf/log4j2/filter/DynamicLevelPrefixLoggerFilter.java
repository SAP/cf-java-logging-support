package com.sap.hcp.cf.log4j2.filter;

import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.slf4j.MDC;

@Plugin(name = "DynamicLevelPrefixLoggerFilter", category = "Core", elementType = "filter", printObject = true)
public class DynamicLevelPrefixLoggerFilter extends AbstractFilter {

    @PluginFactory
    public static DynamicLevelPrefixLoggerFilter createFilter() {
        return new DynamicLevelPrefixLoggerFilter();
    }

    @Override
    public Result filter(LogEvent event) {
        Level level = event.getLevel();
        Level dynamicLevel = getDynamicLevel(event);
        String loggerFqcn = event.getLoggerFqcn();
        String logLevelPackages = getDynamicPackages(event);
        return filter(level, dynamicLevel, loggerFqcn, logLevelPackages);
    }

    private Level getDynamicLevel(LogEvent event) {
        String logLevel = getContextValue(event, DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        return StringUtils.isNotBlank(logLevel) ? Level.getLevel(logLevel) : null;
    }

    private String getContextValue(LogEvent event, String key) {
        ReadOnlyStringMap contextData = event.getContextData();
        return contextData != null ? contextData.getValue(key) : null;
    }

    private String getDynamicPackages(final LogEvent event) {
        String logLevelPackages = getContextValue(event, DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
        return logLevelPackages;
    }

    private Result filter(Level level, Level dynamicLevel, String loggerFqcn, String logLevelPackages) {
        if (dynamicLevel != null && level.isMoreSpecificThan(dynamicLevel) && checkPackages(loggerFqcn,
                                                                                            logLevelPackages)) {
            return Result.ACCEPT;
        }
        return Result.NEUTRAL;
    }

    private boolean checkPackages(String loggerFqcn, String logLevelPackages) {
        if (StringUtils.isNotBlank(logLevelPackages)) {
            for (String current: logLevelPackages.split(",")) {
                if (loggerFqcn.startsWith(current)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        return filter(level, getMdcLevel(), logger.getName(), getMdcPackages());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        return filter(level, getMdcLevel(), logger.getName(), getMdcPackages());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return filter(level, getMdcLevel(), logger.getName(), getMdcPackages());
    }

    private Level getMdcLevel() {
        String mdcLevel = MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        return StringUtils.isNotBlank(mdcLevel) ? Level.getLevel(mdcLevel) : null;
    }

    private String getMdcPackages() {
        return MDC.get(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
    }
}
