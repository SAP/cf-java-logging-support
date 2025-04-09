package com.sap.hcp.cf.logging.servlet.dynlog.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;

/**
 * A {@link DynamicLogLevelProvider} maps an {@link HttpServletRequest} to a {@link DynamicLogLevelConfiguration}.
 * Implementations can be registered via SPI. This allows easy integration with different approaches for dynamic log
 * level configuration.
 */
public interface DynamicLogLevelProvider extends Function<HttpServletRequest, DynamicLogLevelConfiguration> {
}
