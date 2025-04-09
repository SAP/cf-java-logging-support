package com.sap.hcp.cf.logging.servlet.dynlog.api;

/**
 * Basic configuration of dynamic log levels provided by a {@link DynamicLogLevelProvider}.
 *
 * @param level
 *         the minimal log level (severity) at which messages should be logged.
 * @param packages
 *         the package prefix to which the level should be applied to. Leave empty for all packages.
 */
public record DynamicLogLevelConfiguration(String level, String packages) {
    public static final DynamicLogLevelConfiguration EMPTY = new DynamicLogLevelConfiguration(null, null);

}
