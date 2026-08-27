# OpenTelemetry Java Agent Extension for SAP BTP Observability

This module provides an extension for the [OpenTelemetry Java Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/).
The extension scans the service bindings of an application for a generic OTel Collector, SAP Collector as a Service (CaaS), [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging) and [Dynatrace](https://docs.dynatrace.com/docs/setup-and-configuration/setup-on-container-platforms/cloud-foundry/deploy-oneagent-on-sap-cloud-platform-for-application-only-monitoring).
If such a binding is found, the OpenTelemetry Java Agent is configured to ship observability data to those services.
Thus, this extension provides a convenient auto-instrumentation for Java applications running on SAP BTP.

> Note: CaaS currently is an SAP internal only service.

The extension provides the following main features:

* auto-configuration of the generic OpenTelemetry OTLP exporter to any OTel Collector service binding (identified by instance name), SAP Collector as a Service (CaaS) or [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging)
* optional mTLS using client certificate and key read directly from the OTel Collector service binding credentials
* additional exporters for logs, metrics and traces for [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging)
* additional exporter for metrics for [Dynatrace](https://docs.dynatrace.com/docs/setup-and-configuration/setup-on-container-platforms/cloud-foundry/deploy-oneagent-on-sap-cloud-platform-for-application-only-monitoring)
* adding resource attributes describing the CF application
* filtering span attributes by name before export

See the section on [configuration](#configuration) for further details.

## Quickstart Guide

Any Java application can be instrumented with the OpenTelemetry Java Agent and this extension by adding the following arguments to the java command:

```sh
java -javaagent:/path/to/opentelemetry-javaagent-<version>.jar \
     -Dotel.javaagent-extensions=/path/to/cf-java-logging-support-opentelemetry-agent-extension-<version>.jar \
     # your Java application command
```

If you are using Spring Boot, you can bundle both dependencies with the application.
See the Maven pom of the [Spring Boot sample application](../sample-spring-boot/pom.xml) for details.
When deployed to a Cloud Foundry runtime environment, the Spring Boot jar is expanded, so that the agent and extension jar are available during application start.
In that case, the following Java arguments are required:

```sh
java -javaagent:BOOT-INF/lib/opentelemetry-javaagent-<version>.jar \ 
     -Dotel.javaagent.extensions=BOOT-INF/lib/cf-java-logging-support-opentelemetry-agent-extension-<version>.jar \
     # your Java application command
```

> You need to use the OpenTelemetry Java Agent version this extension was built against to avoid compatibility issues.

See the [example manifest](../sample-spring-boot/manifest-otel-javaagent.yml), how this translates into a deployment description.

Once the agent is attached to the JVM with the extension in place, the default `otlp` exporter is automatically configured based on available service bindings:

1. **Generic OTel Collector Binding** (highest priority, explicit opt-in): If any selector property (`sap.otel.generic.cf.binding.name`, `sap.otel.generic.cf.binding.label`, or `sap.otel.generic.cf.binding.tag`) is configured, the extension locates the matching CF service instance and uses it as the OTel Collector endpoint. Optionally uses mTLS credentials read directly from the binding. See [Using a Generic OTel Collector Service Binding](#using-a-generic-otel-collector-service-binding).
2. **CaaS Service Binding** (automatic, preferred): If no explicit binding name is set and a CaaS service binding is found, the `otlp` exporter sends data to the CaaS endpoint.
3. **Cloud Logging Service Binding** (automatic, fallback): If neither of the above applies, the `otlp` exporter sends data to Cloud Logging.

This means **metrics and traces are automatically exported** without additional configuration when either service is bound.
The recommended way to export data to Cloud Logging and Dynatrace is to use the provided exporters explicitly.
This can be achieved via system properties or environment variables:

```sh
-Dotel.logs.exporter=cloud-logging \
-Dotel.metrics.exporter=cloud-logging,dynatrace \
-Dotel.traces.exporter=cloud-logging

#or

export OTEL_LOGS_EXPORTER=cloud-logging
export OTEL_METRICS_EXPORTER=cloud-logging,dynatrace
export OTEL_TRACES_EXPORTER=cloud-logging
java #...
```

For the instrumentation to send observability data to [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging) or [Dynatrace](https://docs.dynatrace.com/docs/setup-and-configuration/setup-on-container-platforms/cloud-foundry/deploy-oneagent-on-sap-cloud-platform-for-application-only-monitoring), the application needs to be bound to a corresponding service instances.
The service instances can be either managed or [user-provided](#using-user-provided-service-instances).

## Configuration

The OpenTelemetry Java Agent supports a wide variety of [configuration options](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/).
As the extension provides configuration via SPI, all its configuration takes lower precedence than other configuration options for OpenTelemetry.
Users can easily overwrite any setting using environment variables or system properties.
The full list of configuration properties provided by the extension is available in the [Configuration Properties Summary](#configuration-properties-summary) section.

### Using the Extension

The extension needs to be started with the OpenTelemetry Java Agent as outlined in the [Quick Start Guide](#quickstart-guide).
You need to enable shipping data either by using the `cloud-logging` exporters for each signal type or `dynatrace` for metrics explicitly.
Multiple different exporters can be configured with comma separation.
Using the custom `cloud-logging` exporter enables you, to use the default `otlp` exporter for different services.

Note, that the `cloud-logging` and `dynatrace` exporters are just facades for the `otlp` exporter to allow configuration of multiple data sinks.
There is no custom network client provided by this extension.

### Configuring the Extension

> Note: This section describes configuration options introduced with version 4.1.0 of the extension.
> Earlier versions use different property names, which are still supported as fallback.
> They will create warning messages during initialization as those properties are deprecated for removal.

The extension itself can be configured by specifying the following system properties:

| Property                                                    | Description                                                                                                                                                                                                                                       | Default Value              |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `sap.otel.generic.cf.binding.name`                        | CF service instance name to use as the **Generic OTel Collector** endpoint. Set exactly one of `name`, `label`, or `tag`; the first non-blank value wins (name → label → tag). When set, this binding takes priority over CaaS and Cloud Logging auto-detection. See [Using a Generic OTel Collector](#using-a-generic-otel-collector-service-binding). | *(absent)*                 |
| `sap.otel.generic.cf.binding.label`                       | CF service label to use as the **Generic OTel Collector** endpoint. Used when `name` is blank.                                                                                                                                                    | *(absent)*                 |
| `sap.otel.generic.cf.binding.tag`                         | CF service tag to use as the **Generic OTel Collector** endpoint. Used when both `name` and `label` are blank.                                                                                                                                    | *(absent)*                 |
| `sap.otel.generic.cf.binding.endpoint-name`               | The credential key to read as the OTLP endpoint URL.                                                                                                                                                                                             | `url`                      |
| `sap.otel.generic.cf.binding.client-cert-name`            | The credential key for the mTLS client certificate (PEM-encoded).                                                                                                                                                                                | `tls.crt`                  |
| `sap.otel.generic.cf.binding.client-key-name`             | The credential key for the mTLS client private key (PEM-encoded).                                                                                                                                                                                | `tls.key`                  |
| `sap.otel.generic.cf.binding.server-ca-name`              | The credential key for the server CA certificate (PEM-encoded). Required when mTLS is active.                                                                                                                                                    | `tls.ca.crt`               |
| `sap.otel.generic.cf.binding.token-name`                  | The credential key for the bearer token. When present, sent as `Authorization: Bearer <token>`.                                                                                                                                                   | `token`                    |
| `sap.otel.generic.cf.binding.protocol`                    | The OTLP protocol. Supported values: `http/protobuf`, `grpc`.                                                                                                                                                                                    | `http/protobuf`            |
| `sap.otel.generic.cf.binding.compression`                 | The OTLP compression algorithm. Supported values: `gzip`, `none`.                                                                                                                                                                               | `gzip`                     |
| `sap.caas.cf.binding.label.value`                           | The label of the managed CaaS service binding to bind to.                                                                                                                                                                                         | `caas-service`             |
| `sap.cloud-logging.cf.binding.label.value`                  | The label of the managed service binding to bind to.                                                                                                                                                                                              | `cloud-logging`            |
| `sap.cloud-logging.cf.binding.tag.value`                    | The tag of any service binding (managed or user-provided) to bind to.                                                                                                                                                                             | `Cloud Logging`            |
| `sap.dynatrace.cf.binding.label.value`                      | The label of the managed service binding to bind to.                                                                                                                                                                                              | `dynatrace`                |
| `sap.dynatrace.cf.binding.tag.value`                        | The tag of any service binding (managed or user-provided) to bind to.                                                                                                                                                                             | `dynatrace`                |
| `sap.dynatrace.cf.binding.token.name`                       | The name of the field containing the Dynatrace API token within the service binding credentials. This is required to send metrics to Dynatrace.                                                                                                   |                            |
| `sap.cloudfoundry.otel.resources.enabled`                   | Whether to add CF resource attributes to all events.                                                                                                                                                                                              | `true`                     |
| `sap.cloudfoundry.otel.resources.format`                    | The semantic convention to follow for the CF resource attributes. Supported values are `SAP` and `OTEL`.                                                                                                                                          | `SAP`                      |

> Each property can also be provided as environment variable, e.g., `sap.cloud-logging.cf.binding.label.value` as `SAP.CLOUD-LOGGING.CF.BINDING.LABEL.VALUE`.

The extension scans the `VCAP_SERVICES` environment variable for CF service bindings in the following order:

1. **Generic OTel Collector binding** (only when any selector property is configured): Finds the service instance matching the `name`, `label`, or `tag` selector (first non-blank wins). Reads the configured endpoint URL credential key (default: `url`) as the OTLP endpoint. Optionally reads mTLS client credentials and server CA certificate using the configured credential keys (defaults: `tls.crt`, `tls.key`, `tls.ca.crt`). A server CA certificate is required when mTLS client credentials are present; the binding is skipped if both mTLS credentials are present but the CA certificate is absent.
2. **CaaS bindings**: Searches for bindings matching the configured label (`sap.caas.cf.binding.label.value`, default: `caas-service`)
3. **Cloud Logging bindings**: If no CaaS binding is found, searches for bindings matching the configured label and tag (`sap.cloud-logging.cf.binding.label.value` and `sap.cloud-logging.cf.binding.tag.value`)

User-provided bindings take precedence over managed bindings.
The first matching binding configures the default OpenTelemetry `otlp` exporter.
Bindings to Cloud Logging and Dynatrace are also used to configure the respective exporters.

### Recommended Agent Configuration

The OpenTelemetry Java Agent offers a lot of configuration options.
The following set of properties is recommended to be used with the extension:

```sh
java -javaagent:/path/to/opentelemetry-javaagent-<version>.jar \
     -Dotel.javaagent-extensions=/path/to/cf-java-logging-support-opentelemetry-agent-extension-<versions>.jar \
     # enable logs \
     -Dotel.logs.exporter=otlp \
     # reroute agent logs to otlp \
     -Dotel.javaagent.logging=application
     # configure logback context \
     -Dotel.instrumentation.logback-appender.experimental.capture-mdc-attributes=* \
     -Dotel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes=true \
     -Dotel.instrumentation.logback-appender.experimental.capture-code-attributes=true \
     -Dotel.instrumentation.logback-appender.experimental-log-attributes=true \
     # Disable large resource attributes
     -Dotel.resource.disabled-keys=process.command_line,process.command_args,process.executable.path
```

The [OpenTelemetry Java Instrumentation project](https://github.com/open-telemetry/opentelemetry-java-instrumentation) provides detailed documentation on the configuration properties for [Logback](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/javaagent) and [Log4j](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/log4j/log4j-appender-2.17/javaagent).

### Filtering Metrics

_This feature was introduced with version 4.1.0 of the extension._

You can filter which metrics are exported to Cloud Logging or Dynatrace by name using the following properties:

| Property                                                                                               | Description                                                                                                     |
|--------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `otel.exporter.cloud-logging.metrics.include.names` or `otel.exporter.dynatrace.metrics.include.names` | A comma-separated list of metric names to be forwarded. This may include a wildcard "*" at the end of the name. |    
| `otel.exporter.cloud-logging.metrics.exclude.names` or `otel.exporter.dynatrace.metrics.exclude.names` | A comma-separated list of metric names to be rejected. This may include a wildcard "*" at the end of the name.  |    

Note, that the `include` filter is applied before the `exclude` filter.
That means, if a metric matches both filters, it will be excluded.
The configuration applies to both the `cloud-logging` and `dynatrace` exporters independently.

### Filtering Span Attributes

_This feature was introduced with version 4.3.0 of the extension._

You can filter which span attributes are exported by name using the following properties:

| Property                                                                              | Description                                                                                                            |
|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `sap.cf.integration.otel.extension.sanitizer.span.attribute.filter.include.names`    | A comma-separated list of span attribute name patterns to be included. This may include a wildcard "*" at the end of the name. |
| `sap.cf.integration.otel.extension.sanitizer.span.attribute.filter.exclude.names`    | A comma-separated list of span attribute name patterns to be excluded. This may include a wildcard "*" at the end of the name. |

The filter is only active when the sanitizer is enabled (`sap.cf.integration.otel.extension.sanitizer.enabled=true`) and at least one of the two properties is set.

The `include` filter is applied before the `exclude` filter.
That means, if a span attribute matches both filters, it will be excluded.

### Configuration Properties Summary

The following table summarizes all configuration properties provided by the extension:

| Property                                                            | Description                                                                                                                                                                                                                                                                                                                                   | Default Value                                           |
|---------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| `otel.exporter.cloud-logging.compression`                           | The compression algorithm to use when exporting logs.                                                                                                                                                                                                                                                                                         | `gzip`                                                  |
| `otel.exporter.cloud-logging.timeout`                               | The maximum duration to wait for Cloud Logging when exporting data.                                                                                                                                                                                                                                                                           | `10000 (from OTel SDK)                                  |
| `otel.exporter.cloud-logging.logs.compression`                      | The compression algorithm to use when exporting logs. Falls back to `otel.exporter.cloud-logging.compression` if not set.                                                                                                                                                                                                                     | `gzip` (from `otel.exporter.cloud-logging.compression`) |
| `otel.exporter.cloud-logging.logs.timeout`                          | The maximum duration to wait for Cloud Logging when exporting logs. Falls back to `otel.exporter.cloud-logging.timeout` if not set.                                                                                                                                                                                                           | `10000` (from OTel SDK)                                 |
| `otel.exporter.cloud-logging.metrics.compression`                   | The compression algorithm to use when exporting metrics. Falls back to `otel.exporter.cloud-logging.compression` if not set.                                                                                                                                                                                                                  | `gzip` (from `otel.exporter.cloud-logging.compression`) |
| `otel.exporter.cloud-logging.metrics.default.histogram.aggregation` | The default histogram aggregation for metrics exported to Cloud Logging. Delegates to the underlying OTLP exporter, supporting all its configurations.                                                                                                                                                                                        | `EXPLICIT_BUCKET_HISTOGRAM` (from OTel SDK)             |
| `otel.exporter.cloud-logging.metrics.exclude.names`                 | A comma-separated list of metric name patterns to be excluded when exporting metrics to Cloud Logging. Wildcard "\*" is only supported at the end of the name. If not set, no metrics are excluded.                                                                                                                                           |                                                         |
| `otel.exporter.cloud-logging.metrics.include.names`                 | A comma-separated list of metric name patterns to be included when exporting metrics to Cloud Logging. Wildcard "\*" is only supported at the end of the name. If not set, all metrics are exported.                                                                                                                                          |                                                         |
| `otel.exporter.cloud-logging.metrics.temporality.preference`        | The preferred aggregation temporality for metrics exported to Cloud Logging. Can be either `cumulative`, `delta`, or `lowmemory`.                                                                                                                                                                                                             | `cumulative`                                            |
| `otel.exporter.cloud-logging.metrics.timeout`                       | The maximum duration to wait for Cloud Logging when exporting metrics. Falls back to `otel.exporter.cloud-logging.timeout` if not set.                                                                                                                                                                                                        | `10000` (from OTel SDK)                                 |
| `otel.exporter.cloud-logging.traces.compression`                    | The compression algorithm to use when exporting traces. Falls back to `otel.exporter.cloud-logging.compression` if not set.                                                                                                                                                                                                                   | `gzip` (from `otel.exporter.cloud-logging.compression`) |
| `otel.exporter.cloud-logging.traces.timeout`                        | The maximum duration to wait for Cloud Logging when exporting traces. Falls back to `otel.exporter.cloud-logging.timeout` if not set.                                                                                                                                                                                                         | `10000` (from OTel SDK)                                 |
| `otel.exporter.dynatrace.metrics.compression`                       | The compression algorithm to use when exporting metrics.                                                                                                                                                                                                                                                                                      | `gzip`                                                  |
| `otel.exporter.dynatrace.metrics.default.histogram.aggregation`     | The default histogram aggregation for metrics exported to Dynatrace. Delegates to the underlying OTLP exporter, supporting all its configurations.                                                                                                                                                                                            | `EXPLICIT_BUCKET_HISTOGRAM` (from OTel SDK)             |
| `otel.exporter.dynatrace.metrics.exclude.names`                     | A comma-separated list of metric name patterns to be excluded when exporting metrics to Dynatrace. Wildcard "\*" is only supported at the end of the name. If not set, no metrics are excluded.                                                                                                                                               |                                                         |
| `otel.exporter.dynatrace.metrics.include.names`                     | A comma-separated list of metric name patterns to be included when exporting metrics to Dynatrace. Wildcard "\*" is only supported at the end of the name. If not set, all metrics are exported.                                                                                                                                              |                                                         |
| `otel.exporter.dynatrace.metrics.temporality.preference`            | The default histogram aggregation for metrics exported to Dynatrace. Delegates to the underlying OTLP exporter, supporting all its configurations. The Dynatrace metrics exporter provides an additional option `always_delta` which always uses delta aggregation temporality. This is also the default behavior if the property is not set. | `always_delta`                                          |
| `otel.exporter.dynatrace.metrics.timeout`                           | The maximum duration to wait for Dynatrace when exporting metrics.                                                                                                                                                                                                                                                                            | `10000` (from OTel SDK)                                 |
| `sap.cf.integration.otel.extension.sanitizer.enabled`                                    | Enables or disables the sanitizer.                                                                                                                                                                                                                                                                                                            | `true`                                                  |
| `sap.cf.integration.otel.extension.sanitizer.span.attribute.filter.exclude.names`        | A comma-separated list of span attribute name patterns to be excluded. Wildcard "\*" is only supported at the end of the name. If not set, no span attributes are excluded. Requires the sanitizer to be enabled and at least one filter property to be set.                                                                                  |                                                         |
| `sap.cf.integration.otel.extension.sanitizer.span.attribute.filter.include.names`        | A comma-separated list of span attribute name patterns to be included. Wildcard "\*" is only supported at the end of the name. If not set, all span attributes are included. Requires the sanitizer to be enabled and at least one filter property to be set.                                                                                 |                                                         |
| `sap.cloudfoundry.otel.resources.enabled`                           | Should Cloud Foundry resource attributes be added to the OpenTelemetry resource?                                                                                                                                                                                                                                                              | `true`                                                  |
| `sap.cloudfoundry.otel.resources.format`                            | Determines the semantic convention used for Cloud Foundry resource attributes names. `SAP` - use SAP specific attribute names (default). `OTEL` - use OpenTelemetry semantic convention attribute names.                                                                                                                                      | `SAP`                                                   |
| `sap.cloud-logging.cf.binding.label.value`                          | The label value used to identify managed Cloud Logging service bindings.                                                                                                                                                                                                                                                                      | `cloud-logging`                                         |
| `sap.cloud-logging.cf.binding.tag.value`                            | The tag value used to identify managed Cloud Logging service bindings.                                                                                                                                                                                                                                                                        | `Cloud Logging`                                         |
| `sap.dynatrace.cf.binding.label.value`                              | The label value used to identify managed Dynatrace service bindings.                                                                                                                                                                                                                                                                          | `dynatrace`                                             |
| `sap.dynatrace.cf.binding.tag.value`                                | The tag value used to identify managed Dynatrace service bindings.                                                                                                                                                                                                                                                                            | `dynatrace`                                             |
| `sap.dynatrace.cf.binding.token.name`                               | The name of the field containing the Dynatrace API token within the service binding credentials.                                                                                                                                                                                                                                              |                                                         |
| `sap.otel.generic.cf.binding.name`                                | CF service instance name to use as a Generic OTel Collector. Set exactly one of `name`, `label`, or `tag`; the first non-blank value wins (name → label → tag). When absent, this feature is disabled and the existing CaaS / Cloud Logging auto-detection is unaffected.                   |                                                         |
| `sap.otel.generic.cf.binding.label`                               | CF service label to use as a Generic OTel Collector. Used when `name` is blank.                                                                                                                                                                                                               |                                                         |
| `sap.otel.generic.cf.binding.tag`                                 | CF service tag to use as a Generic OTel Collector. Used when both `name` and `label` are blank.                                                                                                                                                                                               |                                                         |
| `sap.otel.generic.cf.binding.endpoint-name`                       | The credential key to read as the OTLP endpoint URL.                                                                                                                                                                                                                                          | `url`                                                   |
| `sap.otel.generic.cf.binding.client-cert-name`                    | The credential key for the mTLS client certificate (PEM-encoded).                                                                                                                                                                                                                             | `tls.crt`                                               |
| `sap.otel.generic.cf.binding.client-key-name`                     | The credential key for the mTLS client private key (PEM-encoded).                                                                                                                                                                                                                             | `tls.key`                                               |
| `sap.otel.generic.cf.binding.server-ca-name`                      | The credential key for the server CA certificate (PEM-encoded). Required when mTLS is active.                                                                                                                                                                                                 | `tls.ca.crt`                                            |
| `sap.otel.generic.cf.binding.token-name`                          | The credential key for the bearer token. When present, sent as `Authorization: Bearer <token>`.                                                                                                                                                                                               | `token`                                                 |
| `sap.otel.generic.cf.binding.protocol`                            | The OTLP protocol for the Generic OTel Collector binding. Supported values: `http/protobuf`, `grpc`.                                                                                                                                                                                          | `http/protobuf`                                         |
| `sap.otel.generic.cf.binding.compression`                         | The OTLP compression algorithm for the Generic OTel Collector binding. Supported values: `gzip`, `none`.                                                                                                                                                                                      | `gzip`                                                  |

## Using a Generic OTel Collector Service Binding

_This feature was introduced with version 4.4.0 of the extension._

The extension supports sending observability data to **any OTel Collector** service binding — whether a managed service or a user-provided service — by setting one of the selector properties (`sap.otel.generic.cf.binding.name`, `sap.otel.generic.cf.binding.label`, or `sap.otel.generic.cf.binding.tag`).
This is referred to as the **Generic OTel Collector** path to distinguish it from the CaaS-specific auto-detection.

When any selector property is set, the extension:
1. Locates the CF service instance matching the configured selector (first non-blank among `name` → `label` → `tag`) in `VCAP_SERVICES`.
2. Reads the endpoint URL credential key (configurable via `sap.otel.generic.cf.binding.endpoint-name`, default `url`) as the OTLP endpoint.
3. Optionally reads mTLS client credentials using the configured credential keys (defaults: `tls.crt` and `tls.key`). If both mTLS fields are present, also reads the server CA certificate credential key (default `tls.ca.crt`) as the trusted CA. If mTLS is configured but the CA certificate is absent or blank, the binding is skipped and no exporter is configured.
4. Reads the optional token credential key (default `token`) and sends it as a `Bearer` authorization header. This can be combined with mTLS.

### Minimal setup (URL + token)

Create a user-provided service with at least a `url` field and optionally a `token`:

```bash
cf cups my-otel-collector -p '{"url":"https://my-otel-collector.example.com","token":"<api-token>"}'
```

Configure the extension to use it:

```sh
-Dsap.otel.generic.cf.binding.name=my-otel-collector
```

### Setup with mTLS from binding credentials

Place the PEM-encoded client certificate, private key, and optionally the server CA certificate directly in the user-provided service credentials:

```bash
cf cups my-otel-collector -p '{
  "url": "https://my-otel-collector.example.com",
  "tls.crt": "<PEM client certificate>",
  "tls.key": "<PEM private key>",
  "tls.ca.crt": "<PEM CA certificate (optional)>"
}'
```

Configure the extension to use it:

```sh
-Dsap.otel.generic.cf.binding.name=my-otel-collector
```

The credential fields for the Generic OTel Collector binding are:

| Field name  | Contents                                              | Required |
|-------------|-------------------------------------------------------|----------|
| `url`       | OTLP endpoint URL (e.g. `https://collector.example.com`). | Yes |
| `tls.crt`   | PEM-encoded mTLS client certificate.                  | For mTLS |
| `tls.key`   | PEM-encoded mTLS private key.                         | For mTLS |
| `tls.ca.crt`| PEM-encoded server CA certificate.                    | For mTLS |
| `token`     | Bearer token sent as `Authorization: Bearer <token>`. Can be combined with mTLS. | Optional |

> **Implementation note:** The Generic OTel Collector path is backed by a small, reusable
> `BindingPropertiesSupplier` that is configured with the credential field names and temp-file prefixes listed above.
> The same reusable supplier can be pre-configured for other collector-style bindings by supplying a different set of
> credential field names (endpoint URL, client certificate, client key, server CA certificate and bearer token). This
> is purely an internal refactoring: the observable configuration, credential field names and produced OTLP exporter
> properties for existing bindings are unchanged.

## Using User-Provided Service Instances

### SAP Cloud Logging

The extension provides support not only for managed service instance of [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging) but also for user-provided service instances.
This helps to fine-tune the configuration, e.g., leave out or reconfigure the syslog drain.
Furthermore, this helps on sharing service instances across CF orgs or landscapes.

The extension requires four fields in the user-provided service credentials and needs to be tagged with the `otel.javaagent.extension.sap.cf.binding.cloud-logging.tag` (default: `Cloud Logging`) documented in section [Configuration](#configuration).

| Field name             | Contents                                                                                |
|------------------------|-----------------------------------------------------------------------------------------|
| `ingest-otlp-endpoint` | The OTLP endpoint including port. It will be prefixed with `https://`.                  |
| `ingest-otlp-key`      | The mTLS client key in PCKS#8 format. Line breaks as `\n`.                              |
| `ingest-otlp-cert`     | The mTLS client certificate in PEM format matching the client key. Line breaks as `\n`. |
| `server-ca`            | The trusted mTLS server certificate in PEM format. Line breaks as `\n`.                 |

If you have a [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging) service key, you can generate the required JSON file with jq:

```bash
cf service-key cls test \
| tail -n +2 \
| jq '.credentials | {"ingest-otlp-endpoint":."ingest-otlp-endpoint", "ingest-otlp-cert":."ingest-otlp-cert", "ingest-otlp-key":."ingest-otlp-key", "server-ca":."server-ca"}' \
> ups.json
```

Using this file, you can create the required user-provided service:

```bash
 cf cups <your-service-name> -p ups.json -t "Cloud Logging" 
```

Note, that you can easily feed arbitrary credentials to the extension.
It does not need to be [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging).
You can even change the tag using the configuration parameters of the extension.

### Dynatrace

SAP BTP internally offers a managed Dynatrace service, that is recognized by the extension.
Externally, user-provided service instances need to be created.
The [Dynatrace documentation](https://docs.dynatrace.com/docs/setup-and-configuration/setup-on-container-platforms/cloud-foundry/deploy-oneagent-on-sap-cloud-platform-for-application-only-monitoring) explains, how to generate the necessary access url and tokens.
The extension requires two fields in the user-provided service credentials and needs to be tagged with the `sap.dynatrace.cf.binding.tag.value` (default: `dynatrace`) documented in section [Configuration](#configuration).

| Field name           | Contents                                                                                                                                                            |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `apiurl`             | The Dynatrace API endpoint, e.g. `https://apm.example.com/e/<some-uuid>/api`. This url will be appended with `/v2/otlp/v1/metrics` to create the full endpoint url. |
| `<your_token_field>` | The API token to be used with the above endpoint. Ensure, that it has the required permissions to ingest data over the endpoint.                                    |

Do not forget to configure the name chosen for `<your_token_field>` via the respective configuration property:

```sh
java #... \
-Dsap.dynatrace.cf.binding.token.name=<your_token_field> \
# ...

# or

SAP_DYNATRACE_CF_BINDING_TOKEN_NAME=<your_token_field>
java #...
```

## Implementation Differences between Cloud-Logging and OTLP Exporter

The `cloud-logging` exporter provided by this extension is a facade for the `OtlpGrpcExporter` provided by the OpenTelemetry Java Agent, just like the `otlp` exporter.
The difference is just during the bootstrapping phase.
The main differences are:

* The `cloud-logging` exporter will send data to all found Cloud Logging bindings, while the `otlp` exporter uses the first CaaS or Cloud Logging binding found.
* The `otlp` configuration will write the required certificates and keys to temporary files, which are deleted when the JVM is shut down. The `cloud-logging` exporter will keep the secrets in memory.
* The `cloud-logging` exporter needs to be configured explicitly.
