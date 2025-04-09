package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.helper.DynamicLogLevelHelper;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.api.DynamicLogLevelProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link DynamicLogLevelFilter} provides an adapter to all registered {@link DynamicLogLevelProvider}. For each
 * incoming HTTP requests it calls the providers to obtain a {@link DynamicLogLevelConfiguration} which is applied to
 * the MDC. These parameters are evaluated by the logging filters for Logback and Log4j2.
 */
public class DynamicLogLevelFilter extends AbstractLoggingFilter {

    private static final Set<String> ALLOWED_DYNAMIC_LOGLEVELS =
            new HashSet<>(Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR"));

    private static final Logger LOG = LoggerFactory.getLogger(DynamicLogLevelFilter.class);

    public DynamicLogLevelFilter() {
    }

    @Override
    protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
        for (DynamicLogLevelProvider provider: getDynamicLogLevelProviders()) {
            var config = provider.apply(request);
            if (isValid(config)) {
                MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY, config.level());
                if (config.packages() != null) {
                    MDC.put(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES, config.packages());
                }
                return;
            } else {
                LOG.trace("Invalid dynamic log level token encountered.");
            }

        }
    }

    private static boolean isValid(DynamicLogLevelConfiguration config) {
        if (config == null || config == DynamicLogLevelConfiguration.EMPTY) {
            return false;
        }
        return config.level() != null && ALLOWED_DYNAMIC_LOGLEVELS.contains(config.level());
    }

    @Override
    protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
        MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_KEY);
        MDC.remove(DynamicLogLevelHelper.MDC_DYNAMIC_LOG_LEVEL_PREFIXES);
    }

    private static class DynamicLogLevelProvidersHolder {
        static final Set<DynamicLogLevelProvider> providers = loadProviders();

        private static Set<DynamicLogLevelProvider> loadProviders() {
            return ServiceLoader.load(DynamicLogLevelProvider.class).stream().map(ServiceLoader.Provider::get)
                                .collect(Collectors.toSet());
        }
    }

    // package-private for testing
    static Set<DynamicLogLevelProvider> getDynamicLogLevelProviders() {
        return DynamicLogLevelProvidersHolder.providers;
    }

}
