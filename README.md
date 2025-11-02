# Java Logging Support for Cloud Foundry

[![Build Status](https://travis-ci.com/SAP/cf-java-logging-support.svg?branch=master)](https://travis-ci.com/SAP/cf-java-logging-support)
[![REUSE status](https://api.reuse.software/badge/github.com/SAP/cf-java-logging-support)](https://api.reuse.software/info/github.com/SAP/cf-java-logging-support)

**Warning: The `main` branch was force-pushed on October 30th, 2025.**

If you cloned or checked out this repository before that date, you may encounter issues when pulling new changes. To
resolve this, reset your local `main` branch to match the remote:

```shell
git fetch origin
git checkout main git reset --hard origin/main
```

**Caution:** This will discard any local changes on your `main` branch.

## Summary

This is a collection of support libraries for Java applications (Java 11 and above) that serves three main purposes:

1. Provide means to emit *structured application log messages*
2. Instrument parts of your application stack to *collect request metrics*
3. Allow auto-configuration of OpenTelemetry exporters.

The libraries started out to support applications running on Cloud Foundry.
This integration has become optional.
The library can be used in any runtime environment such as Kubernetes or Kyma.

When we say structured, we actually mean in JSON format.
In that sense, it shares ideas with [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder),
but takes a simpler approach as we want to ensure that these structured messages adhere to standardized formats.
With such standardized formats in place, it becomes much easier to ingest, process and search such messages in log
analysis stacks such as [ELK](https://www.elastic.co/webinars/introduction-elk-stack).

If you're interested in the specifications of these standardized formats, you may want to have a closer look at the
`fields.yml` files in the [beats folder](./cf-java-logging-support-core/beats).

While [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder) is tied
to [logback](http://logback.qos.ch/), we've tried to keep implementation neutral and have implemented the core
functionality on top of [slf4j](http://www.slf4j.org/), but provided implementations for
both [logback](http://logback.qos.ch/) and [log4j2](http://logging.apache.org/log4j/2.x/) (and we're open to
contributions that would support other implementations).

The instrumentation part is currently focusing on
providing [request filters for Java Servlets](http://www.oracle.com/technetwork/java/filters-137243.html), but again,
we're open to contributions for other APIs and frameworks.

Lastly, there is also a project on [node.js logging support](https://github.com/SAP/cf-nodejs-logging-support).

## Features and dependencies

As you can see from the structure of this repository, we're not providing one *uber* JAR that contains everything, but
provide each feature separately. We also try to stay away from wiring up too many dependencies by tagging almost all of
them as *provided.* As a consequence, it's your task to get all runtime dependencies resolved in your application POM
file.

All in all, you should do the following:

1. Make up your mind which features you actually need.
2. Adjust your Maven dependencies accordingly.
3. Pick your favorite logging implementation.
   And
4. Adjust your logging configuration accordingly.

Let's say you want to make use of the *servlet filter* feature, then you need to add the following dependency to your
POM with property `cf-logging-version` referring to the latest nexus version (currently `4.0.0-RC2`):

```xml

<properties>
    <cf-logging-version>4.0.0-RC2</cf-logging-version>
</properties>
```

``` xml
<dependency>
  <groupId>com.sap.hcp.cf.logging</groupId>
  <artifactId>cf-java-logging-support-servlet</artifactId>
  <version>${cf-logging-version}</version>
</dependency>
```

This feature only depends on the servlet API which you have included in your POM anyhow. You can find more information
about the *servlet filter* feature (like e.g. how to adjust the web.xml) in
the [Wiki](https://github.com/SAP/cf-java-logging-support/wiki/Instrumenting-Servlets).

## Implementation variants and logging configurations

The *core* feature (on which all other features rely) is just using the `org.slf4j` API, but to actually get logs
written, you need to pick an implementation feature. As stated above, we have two implementations:

* `cf-java-logging-support-logback` based on [logback](http://logback.qos.ch/), and
* `cf-java-logging-support-log4j2` based on [log4j2](http://logging.apache.org/log4j/2.x/).

Again, we don't include dependencies to those implementation backends ourselves, so you need to provide the
corresponding dependencies in your POM file:

*Using logback:*

``` xml
<dependency>
	<groupId>com.sap.hcp.cf.logging</groupId>
  	<artifactId>cf-java-logging-support-logback</artifactId>
  	<version>${cf-logging-version}</version>
</dependency>

<dependency>
  	<groupId>ch.qos.logback</groupId>
   	<artifactId>logback-classic</artifactId>
   	<version>1.5.20</version>
 </dependency>
```

*Using log4j2:*

``` xml
<dependency>
	<groupId>com.sap.hcp.cf.logging</groupId>
  	<artifactId>cf-java-logging-support-log4j2</artifactId>
  	<version>${cf-logging-version}</version>
</dependency>
<dependency>
	<groupId>org.apache.logging.log4j</groupId>
	<artifactId>log4j-slf4j-impl</artifactId>
	<version>2.25.2</version>
</dependency>
	<dependency>
	<groupId>org.apache.logging.log4j</groupId>
	<artifactId>log4j-core</artifactId>
	<version>2.25.2</version>
</dependency>
```

As they have slightly differ in configuration, you again will need to do that yourself. But we hope that we've found an
easy way to accomplish that. The one thing you have to do is pick our *encoder* in your `logback.xml` if you're using
`logback` or our `layout` in your `log4j2.xml`if you're using `log4j2`.

Here are the minimal configurations you'd need:

*logback.xml*:

``` xml
<configuration debug="false" scan="false">
	<appender name="STDOUT-JSON" class="ch.qos.logback.core.ConsoleAppender">
       <encoder class="com.sap.hcp.cf.logback.encoder.JsonEncoder"/>
    </appender>
    <!-- for local development, you may want to switch to a more human-readable layout -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%date %-5level [%thread] - [%logger] [%mdc] - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="${LOG_ROOT_LEVEL:-WARN}">
       <!-- Use 'STDOUT' instead for human-readable output -->
       <appender-ref ref="STDOUT-JSON" />
    </root>
  	<!-- request metrics are reported using INFO level, so make sure the instrumentation loggers are set to that level -->
    <logger name="com.sap.hcp.cf" level="INFO" />
</configuration>
```

*log4j2.xml:*

``` xml
<Configuration
   status="warn" strict="true">
	<Appenders>
        <Console name="STDOUT-JSON" target="SYSTEM_OUT" follow="true">
            <JsonPatternLayout charset="utf-8"/>
        </Console>
        <Console name="STDOUT" target="SYSTEM_OUT" follow="true">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} [%mdc] - %msg%n"/>
        </Console>
	</Appenders>
  <Loggers>
     <Root level="${LOG_ROOT_LEVEL:-WARN}">
        <!-- Use 'STDOUT' instead for human-readable output -->
        <AppenderRef ref="STDOUT-JSON" />
     </Root>
  	 <!-- request metrics are reported using INFO level, so make sure the instrumentation loggers are set to that level -->
     <Logger name="com.sap.hcp.cf" level="INFO"/>
  </Loggers>
</Configuration>
```

## Dynamic Log Levels

This library provides the possibility to change the log-level threshold for a
single thread by adding a token in the header of a request. A detailed
description about how to apply this feature can be found
[here](https://github.com/SAP/cf-java-logging-support/wiki/Dynamic-Log-Levels).

## Logging Stacktraces

Stacktraces can be logged within one log message. Further details can be found
[here](https://github.com/SAP/cf-java-logging-support/wiki/Logging-Stack-Traces).

## Sample Applications

In order to illustrate how the different features are used, this repository includes one sample application:

* a Spring Boot implementation in the [./sample-spring-boot folder](./sample-spring-boot)

## Documentation

More info on the actual implementation can be found in the [Wiki](https://github.com/SAP/cf-java-logging-support/wiki).

## Licensing

Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party
components and their licensing/copyright information is available via
the [REUSE](https://api.reuse.software/info/github.com/SAP/cf-java-logging-support) tool.
