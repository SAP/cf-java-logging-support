package com.sap.hcf.cf.logging.opentelemetry.agent.ext.config;

import java.time.Duration;
import java.util.List;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.config.ConfigProperty.*;

public interface ExtensionConfigurations {

    interface EXPORTER {
        interface CLOUD_LOGGING {
            interface GENERAL {
                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.compression}.</p>
                 * <p>The compression algorithm to use when exporting logs. Default is {@code "gzip"}.</p>
                 */
                ConfigProperty<String> COMPRESSION =
                        stringValued("otel.exporter.cloud-logging.compression").withDefaultValue("gzip").build();

                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.timeout}.</p>
                 * <p>The maximum duration to wait for Cloud Logging when exporting data.</p>
                 */
                ConfigProperty<Duration> TIMEOUT = durationValued("otel.exporter.cloud-logging.timeout").build();
            }

            interface LOGS {
                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.logs.compression}.</p>
                 * <p>The compression algorithm to use when exporting logs. Default is {@code "gzip"}. Falls back to
                 * {@code otel.exporter.cloud-logging.compression} if not set.</p>
                 */
                ConfigProperty<String> COMPRESSION =
                        stringValued("otel.exporter.cloud-logging.logs.compression").withFallback(
                                EXPORTER.CLOUD_LOGGING.GENERAL.COMPRESSION).withDefaultValue("gzip").build();

                /**
                 * <p>{@code otel.exporter.cloud-logging.logs.timeout}.</p>
                 * <p>The maximum duration to wait for Cloud Logging when exporting logs. Falls back to
                 * {@code otel.exporter.cloud-logging.timeout} if not set.</p>
                 */
                ConfigProperty<Duration> TIMEOUT =
                        durationValued("otel.exporter.cloud-logging.logs.timeout").withFallback(
                                EXPORTER.CLOUD_LOGGING.GENERAL.TIMEOUT).build();
            }

            interface METRICS {
                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.metrics.compression}.</p>
                 * <p>The compression algorithm to use when exporting metrics. Default is {@code "gzip"}. Falls back to
                 * {@code otel.exporter.cloud-logging.compression} if not set.</p>
                 */
                ConfigProperty<String> COMPRESSION =
                        stringValued("otel.exporter.cloud-logging.metrics.compression").withFallback(
                                EXPORTER.CLOUD_LOGGING.GENERAL.COMPRESSION).withDefaultValue("gzip").build();

                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.metrics.default.histogram.aggregation}.</p>
                 * <p>The default histogram aggregation for metrics exported to Cloud Logging. Delegates to the
                 * underlying OTLP exporter, supporting all its configurations.</p>
                 */
                ConfigProperty<String> DEFAULT_HISTOGRAM_AGGREGATION =
                        stringValued("otel.exporter.cloud-logging.metrics.default.histogram.aggregation").build();

                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.metrics.exclude.names}.</p>
                 * <p>A comma-seperated list of metric name patterns to be excluded when exporting metrics to Cloud
                 * Logging. Wildcard "*" is only supported at the end of the name. If not set, no metrics are
                 * excluded.</p>
                 */
                ConfigProperty<List<String>> EXCLUDE_NAMES =
                        listValued("otel.exporter.cloud-logging.metrics.exclude.names").build();

                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.metrics.include.names}.</p>
                 * <p>A comma-seperated list of metric name patterns to be included when exporting metrics to Cloud
                 * Logging. Wildcard "*" is only supported at the end of the name. If not set, all metrics are
                 * exported.</p>
                 */
                ConfigProperty<List<String>> INCLUDE_NAMES =
                        listValued("otel.exporter.cloud-logging.metrics.include.names").build();

                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.metrics.temporality.preference}.</p>
                 * <p>The preferred aggregation temporality for metrics exported to Cloud Logging. Can be either
                 * {@code "cumulative"}, {@code "delta"}, or {@code "lowmemory"}. Default is {@code "cumulative"}.</p>
                 */
                ConfigProperty<String> TEMPORALITY_PREFERENCE =
                        stringValued("otel.exporter.cloud-logging.metrics.temporality.preference").withDefaultValue(
                                "cumulative").build();
                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.metrics.timeout}.</p>
                 * <p>The maximum duration to wait for Cloud Logging when exporting metrics. Falls back to
                 * {@code otel.exporter.cloud-logging.timeout} if not set.</p>
                 */
                ConfigProperty<Duration> TIMEOUT =
                        durationValued("otel.exporter.cloud-logging.metrics.timeout").withFallback(
                                EXPORTER.CLOUD_LOGGING.GENERAL.TIMEOUT).build();
            }

            interface TRACES {
                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.traces.compression}.</p>
                 * <p>The compression algorithm to use when exporting traces. Default is {@code "gzip"}. Falls back to
                 * {@code otel.exporter.cloud-logging.compression} if not set.</p>
                 */
                ConfigProperty<String> COMPRESSION =
                        stringValued("otel.exporter.cloud-logging.traces.compression").withFallback(
                                EXPORTER.CLOUD_LOGGING.GENERAL.COMPRESSION).withDefaultValue("gzip").build();

                /**
                 * <p>Parses {@code otel.exporter.cloud-logging.traces.timeout}.</p>
                 * <p>The maximum duration to wait for Cloud Logging when exporting traces. Falls back to
                 * {@code otel.exporter.cloud-logging.timeout} if not set.</p>
                 */
                ConfigProperty<Duration> TIMEOUT =
                        durationValued("otel.exporter.cloud-logging.traces.timeout").withFallback(
                                EXPORTER.CLOUD_LOGGING.GENERAL.TIMEOUT).build();
            }
        }

        interface DYNATRACE {
            interface METRICS {
                /**
                 * <p>Parses {@code otel.exporter.dynatrace.metrics.compression}.</p>
                 * <p>The compression algorithm to use when exporting metrics. Default is {@code "gzip"}.</p>
                 */
                ConfigProperty<String> COMPRESSION =
                        stringValued("otel.exporter.dynatrace.metrics.compression").withDefaultValue("gzip").build();

                /**
                 * <p>Parses {@code otel.exporter.dynatrace.metrics.default.histogram.aggregation}.</p>
                 * <p>The default histogram aggregation for metrics exported to Dynatrace. Delegates to the underlying
                 * OTLP exporter, supporting all its configurations.</p>
                 */
                ConfigProperty<String> DEFAULT_HISTOGRAM_AGGREGATION =
                        stringValued("otel.exporter.dynatrace.metrics.default.histogram.aggregation").build();

                /**
                 * <p>Parses {@code otel.exporter.dynatrace.metrics.exclude.names}.</p>
                 * <p>A comma-seperated list of metric name patterns to be excluded when exporting metrics to
                 * CDynatrace. Wildcard "*" is only supported at the end of the name. If not set, no metrics are
                 * excluded.</p>
                 */
                ConfigProperty<List<String>> EXCLUDE_NAMES =
                        listValued("otel.exporter.dynatrace.metrics.exclude.names").build();

                /**
                 * <p>Parses {@code otel.exporter.dynatrace.metrics.include.names}.</p>
                 * <p>A comma-seperated list of metric name patterns to be included when exporting metrics to
                 * Dynatrace. Wildcard "*" is only supported at the end of the name. If not set, all metrics are
                 * exported.</p>
                 */
                ConfigProperty<List<String>> INCLUDE_NAMES =
                        listValued("otel.exporter.dynatrace.metrics.include.names").build();

                /**
                 * <p>Parses {@code otel.exporter.dynatrace.metrics.default.histogram.aggregation}.</p>
                 * <p>The default
                 * histogram aggregation for metrics exported to Dynatrace. Delegates to the underlying OTLP exporter,
                 * supporting all its configurations.</p>
                 * <p>The Dynatrace metrics exporter provides an additional option {@code "always_delta"} which always
                 * uses delta aggregation temporality. This is also the default behavior if the property is not
                 * set.</p>
                 */
                ConfigProperty<String> TEMPORALITY_PREFERENCE =
                        stringValued("otel.exporter.dynatrace.metrics.temporality.preference").withDefaultValue(
                                "always_delta").build();

                /**
                 * <p>Parses {@code otel.exporter.dynatrace.metrics.timeout}.</p>
                 * <p>The maximum duration to wait for Dynatrace when exporting metrics.</p>
                 */
                ConfigProperty<Duration> TIMEOUT = durationValued("otel.exporter.dynatrace.metrics.timeout").build();

            }
        }
    }

    interface EXTENSION {
        interface SANITIZER {
            /**
             * <p>Parses {@code sap.cf.integration.otel.extension.sanitizer.enabled}.</p>
             * <p>Enables or disables the sanitizer. Default is {@code true}.</p>
             */
            ConfigProperty<Boolean> ENABLED =
                    booleanValued("sap.cf.integration.otel.extension.sanitizer.enabled").withDefaultValue(true).build();
        }
    }

    interface RESOURCE {
        interface CLOUD_FOUNDRY {
            /**
             * <p>Parses {@code sap.cloudfoundry.otel.resources.enabled}.</p>
             * <p>Should Cloud Foundry resource attributes be added to the OpenTelemetry resource? Default is
             * {@code true}.</p>
             */
            ConfigProperty<Boolean> ENABLED = booleanValued("sap.cloudfoundry.otel.resources.enabled").withFallback(
                    DEPRECATED.RESOURCE.CLOUD_FOUNDRY.ENABLED).withDefaultValue(true).build();
            /**
             * <p>Parses {@code sap.cloudfoundry.otel.resources.format}.</p>
             * <p>Determines the semantic convention used for Cloud Foundry resource attributes names.</p>
             * <ul>
             * <li>{@code "SAP"} - use SAP specific attribute names (default)</li>
             * <li>{@code "OTEL"} - use OpenTelemetry semantic convention attribute names</li>
             * </ul>
             */
            ConfigProperty<String> FORMAT = stringValued("sap.cloudfoundry.otel.resources.format").withFallback(
                    DEPRECATED.RESOURCE.CLOUD_FOUNDRY.FORMAT).withDefaultValue("SAP").build();
        }
    }

    interface RUNTIME {
        interface CLOUD_FOUNDRY {
            interface SERVICE {
                interface CLOUD_LOGGING {
                    /**
                     * <p>Parses {@code sap.cloud-logging.cf.binding.label.value}.</p>
                     * <p>The label value used to identify managed Cloud Logging service bindings. Default is
                     * {@code "cloud-logging"}.</p>
                     */
                    ConfigProperty<String> LABEL =
                            stringValued("sap.cloud-logging.cf.binding.label.value").withFallback(
                                                                                            DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.LABEL_OTEL)
                                                                                    .withDefaultValue("cloud-logging")
                                                                                    .build();
                    /**
                     * <p>Parses {@code sap.cloud-logging.cf.binding.tag.value}.</p>
                     * <p>The tag value used to identify managed Cloud Logging service bindings. Default is
                     * {@code "Cloud Logging"}.</p>
                     */
                    ConfigProperty<String> TAG = stringValued("sap.cloud-logging.cf.binding.tag.value").withFallback(
                            DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.CLOUD_LOGGING.TAG_OTEL).withDefaultValue(
                            "Cloud Logging").build();
                }

                interface DYNATRACE {
                    /**
                     * <p>Parses {@code sap.dynatrace.cf.binding.label.value}.</p>
                     * <p>The label value used to identify managed Dynatrace service bindings. Default is
                     * {@code "dynatrace"}.</p>
                     */
                    ConfigProperty<String> LABEL = stringValued("sap.dynatrace.cf.binding.label.value").withFallback(
                                                                                                               DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.LABEL_OTEL).withDefaultValue("dynatrace")
                                                                                                       .build();
                    /**
                     * <p>Parses {@code sap.dynatrace.cf.binding.tag.value}.</p>
                     * <p>The tag value used to identify managed Dynatrace service bindings. Default is
                     * {@code "dynatrace"}.</p>
                     */
                    ConfigProperty<String> TAG = stringValued("sap.dynatrace.cf.binding.tag.value").withFallback(
                                                                                                           DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TAG_OTEL).withDefaultValue("dynatrace")
                                                                                                   .build();

                    ConfigProperty<String> TOKEN_NAME =
                            stringValued("sap.dynatrace.cf.binding.metrics.token.name").withFallback(
                                    DEPRECATED.RUNTIME.CLOUD_FOUNDRY.SERVICE.DYNATRACE.TOKEN_NAME_OTEL).build();
                }

            }
        }
    }

    @Deprecated(since = "4.1.0", forRemoval = true)
    interface DEPRECATED {
        interface RESOURCE {
            interface CLOUD_FOUNDRY {
                ConfigProperty<Boolean> ENABLED =
                        booleanValued("otel.javaagent.extension.sap.cf.resource.enabled").setDeprecated(true)
                                                                                         .withDefaultValue(true)
                                                                                         .build();
                ConfigProperty<String> FORMAT =
                        stringValued("otel.javaagent.extension.sap.cf.resource.format").setDeprecated(true)
                                                                                       .withDefaultValue("SAP").build();
            }
        }

        interface RUNTIME {
            interface CLOUD_FOUNDRY {
                interface SERVICE {
                    interface CLOUD_LOGGING {
                        ConfigProperty<String> LABEL_SAP =
                                stringValued("com.sap.otel.extension.cloud-logging.label").setDeprecated(true)
                                                                                          .withDefaultValue(
                                                                                                  "cloud-logging")
                                                                                          .build();
                        ConfigProperty<String> LABEL_OTEL = stringValued(
                                "otel.javaagent.extension.sap.cf.binding.cloud-logging.label").setDeprecated(true)
                                                                                              .withFallback(LABEL_SAP)
                                                                                              .build();
                        ConfigProperty<String> TAG_SAP =
                                stringValued("com.sap.otel.extension.cloud-logging.tag").setDeprecated(true)
                                                                                        .withDefaultValue(
                                                                                                "Cloud Logging")
                                                                                        .build();
                        ConfigProperty<String> TAG_OTEL =
                                stringValued("otel.javaagent.extension.sap.cf.binding.cloud-logging.tag").setDeprecated(
                                        true).withFallback(TAG_SAP).build();
                    }

                    interface DYNATRACE {
                        ConfigProperty<String> LABEL_OTEL =
                                stringValued("otel.javaagent.extension.sap.cf.binding.dynatrace.label").setDeprecated(
                                        true).withDefaultValue("dynatrace").build();
                        ConfigProperty<String> TAG_OTEL =
                                stringValued("otel.javaagent.extension.sap.cf.binding.dynatrace.tag").setDeprecated(
                                        true).withDefaultValue("dynatrace").build();

                        ConfigProperty<String> TOKEN_NAME_OTEL = stringValued(
                                "otel.javaagent.extension.sap.cf.binding.dynatrace.metrics.token-name").setDeprecated(
                                true).build();
                    }

                    interface USER_PROVIDED {
                        ConfigProperty<String> LABEL_OTEL = stringValued(
                                "otel.javaagent.extension.sap.cf.binding.user-provided.label").setDeprecated(true)
                                                                                              .withDefaultValue(
                                                                                                      "user-provided")
                                                                                              .build();
                    }
                }
            }
        }
    }
}
