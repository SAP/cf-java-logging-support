package com.sap.hcp.cf.logging.servlet.filter;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.hcp.cf.logging.servlet.dynlog.DynLogEnvironment;
import com.sap.hcp.cf.logging.servlet.dynlog.DynamicLogLevelConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.DynamicLogLevelProcessor;

/**
 * <p>
 * The {@link DynamicLogLevelFilter} provides an adapter to an
 * {@link DynamicLogLevelProcessor}. It extracts the JWT from the HTTP header
 * and hands it over to the {@link DynamicLogLevelProcessor} for verification
 * and modification of the MDC.
 * </p>
 * 
 * <p>
 * Setup and processing of these tokens can be changed with own implementations
 * of {@link DynamicLogLevelConfiguration} and {@link DynamicLogLevelProcessor}.
 * For integration provide a subclass of {@link DynamicLogLevelFilter} and
 * overwrite {@link DynamicLogLevelFilter#getConfiguration()} and
 * {@link DynamicLogLevelFilter#getProcessor()}. Alternatively you can use the
 * different constructors to provide a custom configuration and processor
 * </p>
 */
public class DynamicLogLevelFilter extends AbstractLoggingFilter {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicLogLevelFilter.class);

    private ConcurrentInitializer<DynamicLogLevelConfiguration> configuration;
    private ConcurrentInitializer<DynamicLogLevelProcessor> processor;

    /**
     * Provides dynamic log levels by reading the configuration from environment
     * variables and using the default {@link DynamicLogLevelProcessor}.
     */
    public DynamicLogLevelFilter() {
        this(() -> new DynLogEnvironment());
    }

    /**
     * Provides dynamic log levels by using the given configuration and the
     * default {@link DynamicLogLevelProcessor}.
     * 
     * @param configuration
     *            a {@link ConcurrentInitializer} for the configuration, you can
     *            use a lambda: {@code () -> config}
     */
    public DynamicLogLevelFilter(ConcurrentInitializer<DynamicLogLevelConfiguration> configuration) {
        this.configuration = new LazyInitializer<DynamicLogLevelConfiguration>() {

            @Override
            protected DynamicLogLevelConfiguration initialize() throws ConcurrentException {
                return configuration.get();
            }
        };
        this.processor = new LazyInitializer<DynamicLogLevelProcessor>() {

            @Override
            protected DynamicLogLevelProcessor initialize() throws ConcurrentException {
                return getConfiguration().map(DynamicLogLevelConfiguration::getRsaPublicKey).map(
                                                                                                 DynamicLogLevelProcessor::new)
                                         .orElse(null);
            }
        };
    }

    /**
     * Provides dynamic log levels by using the given configuration and
     * processor.
     * 
     * @param configuration
     *            a {@link ConcurrentInitializer} for the configuration, you can
     *            use a lambda: {@code () -> config}
     * @param processor
     *            a {@link ConcurrentInitializer} for the processor, you can use
     *            a lambda: {@code () -> processor}
     */
    public DynamicLogLevelFilter(ConcurrentInitializer<DynamicLogLevelConfiguration> configuration,
                                 ConcurrentInitializer<DynamicLogLevelProcessor> processor) {
        this.configuration = configuration;
        this.processor = processor;
    }

    /**
     * Get the current {@link DynamicLogLevelConfiguration}. Overload this
     * method for customization when you cannot use the constructors.
     * 
     * @return an {@link Optional} of the current configuration
     */
    protected Optional<DynamicLogLevelConfiguration> getConfiguration() {
        try {
            return Optional.ofNullable(configuration.get());
        } catch (ConcurrentException cause) {
            LOG.debug("Cannot initialize dynamic log level environment. Will continue without this feature", cause);
            return Optional.empty();
        }
    }

    /**
     * Get the current {@link DynamicLogLevelProcessor}. Overload this method
     * for customization when you cannot use the constructors.
     * 
     * @return an {@link Optional} of the current processor
     */
    protected Optional<DynamicLogLevelProcessor> getProcessor() {
        try {
            return Optional.ofNullable(processor.get());
        } catch (ConcurrentException cause) {
            LOG.debug("Cannot initialize dynamic log level processor. Will continue without this feature", cause);
        }
        return Optional.empty();
    }

    @Override
    protected void beforeFilter(HttpServletRequest request, HttpServletResponse response) {
        getProcessor().ifPresent(processor -> extractHeader(request).ifPresent(processor::copyDynamicLogLevelToMDC));
    }

    private Optional<String> extractHeader(HttpServletRequest request) {
        return getConfiguration().map(cfg -> cfg.getDynLogHeaderValue(request));
    }

    @Override
    protected void cleanup(HttpServletRequest request, HttpServletResponse response) {
        getProcessor().ifPresent(DynamicLogLevelProcessor::removeDynamicLogLevelFromMDC);
    }

}
