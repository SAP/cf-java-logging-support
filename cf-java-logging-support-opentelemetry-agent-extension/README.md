# OpenTelemetry Java Agent Extension for SAP Cloud Logging

This module provides an extension for the [OpenTelemetry Java Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/).
The extension scans the service bindings of an application for [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging).
If such a binding is found, the OpenTelemetry Java Agent is configured to ship observability data to that service.
Thus, this extension provides a convenient auto-instrumentation for Java applications running on SAP BTP.

The extension provides the following main features:

* additional exporters for logs, metrics and traces for [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging)
* auto-configuration of the generic OpenTelemetry connection to [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging)
* adding resource attributes describing the CF application

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

> You need to use OpenTelemetry Java Agent 1.29.0+, so it provides jackson-databind to the extension.

See the [example manifest](../sample-spring-boot/manifest-otel-javaagent.yml), how this translates into a deployment description.

Once the agent is attached to the JVM with the ectension in place, there are two ways, which can be used to send data to [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging):

1. Use the `cloud-logging` exporters explicitly as provided by the extension.
This can be achieved via system properties or environment variables:
```sh
-Dotel.logs.exporter=cloud-logging \
-Dotel.metrics.exporter=cloud-logging \
-Dotel.traces.exporter=cloud-logging

#or

export OTEL_LOGS_EXPORTER=cloud-logging
export OTEL_METRICS_EXPORTER=cloud-logging
export OTEL_TRACES_EXPORTER=cloud-logging
java #...
```

2. Use the default `otlp` exporter with the provided default configuration from the extension:

```sh
-Dotel.logs.exporter=otlp \
-Dotel.metrics.exporter=otlp # default value \
-Dotel.traces.exporter=otlp # default value

#or

export OTEL_LOGS_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=otlp # default value
export OTEL_TRACES_EXPORTER=otlp # default value
java #...
```

Note, that the OpenTelemetry Java Agent currently sends traces and metrics by default using the `otlp` exporter.
That means, without any configuration the agent with the extension will forward metrics and traces to [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging).
See TODO for the difference between `cloud-logging` and `otlp` exporters.
The benefit of the `cloud-logging` exporter is, that it can be combined with a different configuration of the `otlp` exporter.

For the instrumentation to send observability data to [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging), the application needs to be bound to a corresponding service instance.
The service instance can be either managed or [user-provided](#using-user-provided-service-instances).

## Configuration

The OpenTelemetry Java Agent supports a wide variety of [configuration options](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/).
As the extension provides configuration via SPI, all its configuration takes lower precedence than other configuration options for OpenTelemetry.
Users can easily overwrite any setting using environment variables or system properties.

### Using the Extension

The extension needs to be started with the OpenTelemetry Java Agent as outlined in the [Quick Start Guide](#quickstart-guide).
You need to enable shipping data either by using the `cloud-logging` exporters or relying on the `otlp` exporters for each signal type.
Multiple different exporters can be configured with comma separation.
Using the custom `cloud-logging` exporter will enable you, to use the default `otlp` exporter for different services.
The extension will configure a default endpoint and credentials for the `otlp` endpoints, so no further configuration is required.

Note, that the `cloud-logging` exporter is just a facade for the `otlp` exporter to allow configuration of multiple data sinks.
There is no custom network client provided by this extension.

### Configuring the Extension

The extension itself can be configured by specifying the following system properties:

| Property | Default Value | Comment |
|----------|---------------|---------|
| `otel.javaagent.extension.sap.cf.binding.cloud-logging.label` or `com.sap.otel.extension.cloud-logging.label` | `cloud-logging` | The label of the managed service binding to bind to. |
| `otel.javaagent.extension.sap.cf.binding.cloud-logging.tag` or `com.sap.otel.extension.cloud-logging.tag` | `Cloud Logging` | The tag of any service binding (managed or user-provided) to bind to. |
| `otel.javaagent.extension.sap.cf.binding.user-provided.label` | `user-provided` | The label of a user-provided service binding to bind to. Note, this label is defined by the Cloud Foundry instance. |
| `otel.javaagent.extension.sap.cf.resource.enabled` | `true` | Whether to add CF resource attributes to all events. |

> The `otel.javaagent.extension.sap.*` properties are preferred over the `com.sap.otel.extension.*` properties, which are kept for compatibility. 
Each `otel.javaagent.extension.sap.*` property can also be provided as environment variable `OTEL_JAVAAGENT_EXTENSION_SAP_*`.

The extension will scan the environment variable `VCAP_SERVICES` for CF service bindings.
User-provided bindings will take precedence over managed bindings of the configured label ("cloud-logging" by default).
All matching bindings are filtered for the configured tag ("Cloud Logging" by default).
The first binding will be taken for configuration for the OpenTelemetry exporter.
Preferring user-provided services over managed service instances allows better control of the binding properties, e.g. syslog drains.

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
     -Dotel.experimental.resource.disabled-keys=process.command_line,process.command_args,process.executable.path
```

The [OpenTelemetry Java Instrumentation project](https://github.com/open-telemetry/opentelemetry-java-instrumentation) provides detailed documentation on the configuration properties for [Logback](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/javaagent) and [Log4j](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/log4j/log4j-appender-2.17/javaagent).

## Using User-Provided Service Instances

The extension provides support not only for managed service instance of [SAP Cloud Logging](https://discovery-center.cloud.sap/serviceCatalog/cloud-logging) but also for user-provided service instances.
This helps to fine-tune the configuration, e.g. leave out or reconfigure the syslog drain.
Furthermore, this helps on sharing service instances across CF orgs or landscapes.

The extension requires four fields in the user-provided service credentials and needs to be tagged with the `com.sap.otel.extension.cloud-logging.tag` (default: `Cloud Logging`) documented in section [Configuration](#configuration).

| Field name | Contents |
|------------|---------|
| `ingest-otlp-endpoint` | The OTLP endpoint including port. It will be prefixed with `https://`. |
| `ingest-otlp-key` | The mTLS client key in PCKS#8 format. Line breaks as `\n`. |
| `ingest-otlp-cert`| The mTLS client certificate in PEM format matching the client key. Line breaks as `\n`. |
| `server-ca` | The trusted mTLS server certificate in PEM format. Line breaks as `\n`. |

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

## Implementation Differences between Cloud-Logging and OTLP Exporter

The `cloud-logging` exporter provided by this extension is a facade for the `OtlpGrpcExporter` provided by the OpenTelemetry Java Agent, just like the `otlp` exporter.
The difference is just during the bootstrapping phase.
The main differences are:

* The `cloud-logging` exporter will send data to all found bindings to SAP Cloud Logging.
The auto-instrumentation of the `otlp` exporter will just configure the first binding it finds priotizing user-provided services.
* The `otlp` configuration will write the required certificates and keys to temporary files, which are deleted when the JVM is shut down. The `cloud-logging` exporter will keep the secrets in memory.
* Since the `otlp` exporter is the default for traces and metrics, just attaching the extension and binding to Cloud Logging will result in metrics and traces being forwarded. 
The `cloud-logging` exporter needs to be configured explictly as does the `otlp` exporter for logs.
