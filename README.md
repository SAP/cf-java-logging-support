# Java Logging Support for Cloud Foundry

[![Build Status](https://travis-ci.com/SAP/cf-java-logging-support.svg?branch=master)](https://travis-ci.com/SAP/cf-java-logging-support)
[![REUSE status](https://api.reuse.software/badge/github.com/SAP/cf-java-logging-support)](https://api.reuse.software/info/github.com/SAP/cf-java-logging-support)

**Note: The default branch has been renamed from master to main.**
Please execute on your local repository:

```
git branch -m master main
git fetch origin
git branch -u origin/main main
git remote set-head origin -a
```

## Summary

This is a collection of support libraries for Java applications (Java 8 and above) that serves three main purposes: 

1. Provide means to emit *structured application log messages* 
2. Instrument parts of your application stack to *collect request metrics* 
3. Allow production of *custom metrics*.

The libraries started out to support applications running on Cloud Foundry.
This integration has become optional.
The library can be used in any runtime environment such as Kubernetes or Kyma.

When we say structured, we actually mean in JSON format. 
In that sense, it shares ideas with [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder), but takes a simpler approach as we want to ensure that these structured messages adhere to standardized formats. 
With such standardized formats in place, it becomes much easier to ingest, process and search such messages in log analysis stacks such as [ELK](https://www.elastic.co/webinars/introduction-elk-stack).

If you're interested in the specifications of these standardized formats, you may want to have a closer look at the `fields.yml` files in the [beats folder](./cf-java-logging-support-core/beats).

While [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder) is tied to [logback](http://logback.qos.ch/), we've tried to keep implementation neutral and have implemented the core functionality on top of [slf4j](http://www.slf4j.org/), but provided implementations for both [logback](http://logback.qos.ch/) and [log4j2](http://logging.apache.org/log4j/2.x/) (and we're open to contributions that would support other implementations).

The instrumentation part is currently focusing on providing [request filters for Java Servlets](http://www.oracle.com/technetwork/java/filters-137243.html), but again, we're open to contributions for other APIs and frameworks.

The custom metrics instrumentation allows users to easily define and emit custom metrics. 
The different modules configure all necessary components and make it possible to define custom metrics with minimal code change.
Once collected, custom metrics are sent as special log message.

Lastly, there is also a project on [node.js logging support](https://github.com/SAP/cf-nodejs-logging-support).

## Features and dependencies

As you can see from the structure of this repository, we're not providing one *uber* JAR that contains everything, but provide each feature separately. We also try to stay away from wiring up too many dependencies by tagging almost all of them as *provided.* As a consequence, it's your task to get all runtime dependencies resolved in your application POM file.

All in all, you should do the following:

1. Make up your mind which features you actually need.
2. Adjust your Maven dependencies accordingly.
3. Pick your favorite logging implementation.
And
4. Adjust your logging configuration accordingly.

Let's say you want to make use of the *servlet filter* feature, then you need to add the following dependency to your POM with property `cf-logging-version` referring to the latest nexus version (currently `3.8.6`):

```xml
<properties>
	<cf-logging-version>3.8.6</cf-logging-version>
</properties>
```

``` xml

<dependency>
  <groupId>com.sap.hcp.cf.logging</groupId>
  <artifactId>cf-java-logging-support-servlet</artifactId>
  <version>${cf-logging-version}</version>
</dependency>
```

This feature only depends on the servlet API which you have included in your POM anyhow. You can find more information about the *servlet filter* feature (like e.g. how to adjust the web.xml) in the [Wiki](https://github.com/SAP/cf-java-logging-support/wiki/Instrumenting-Servlets).
Note, that we provide two different servlet instrumentations:

* cf-java-logging-support-servlet linked against `javax.servlet`
* cf-java-logging-support-servlet-jakarta linked against `jakarta.servlet`

Both modules build on the same code but use the respective API.

If you want to use the `custom metrics`, just define the following dependency:

* Spring Boot Support:

``` xml

<dependency>
  <groupId>com.sap.hcp.cf.logging</groupId>
  <artifactId>cf-custom-metrics-clients-spring-boot</artifactId>
  <version>${cf-logging-version}</version>
</dependency>
```

* Plain Java Support:

``` xml

<dependency>
  <groupId>com.sap.hcp.cf.logging</groupId>
  <artifactId>cf-custom-metrics-clients-java</artifactId>
  <version>${cf-logging-version}</version>
</dependency>
```

## Implementation variants and logging configurations

The *core* feature (on which all other features rely) is just using the `org.slf4j` API, but to actually get logs written, you need to pick an implementation feature. As stated above, we have two implementations:

* `cf-java-logging-support-logback` based on [logback](http://logback.qos.ch/), and
* `cf-java-logging-support-log4j2` based on [log4j2](http://logging.apache.org/log4j/2.x/).

Again, we don't include dependencies to those implementation backends ourselves, so you need to provide the corresponding dependencies in your POM file:

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
   	<version>1.2.11</version>
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
	<version>2.20.0</version>
</dependency>
	<dependency>
	<groupId>org.apache.logging.log4j</groupId>
	<artifactId>log4j-core</artifactId>
	<version>2.20.0</version>
</dependency>
```

As they have slightly differ in configuration, you again will need to do that yourself. But we hope that we've found an easy way to accomplish that. The one thing you have to do is pick our *encoder* in your `logback.xml` if you're using `logback` or our `layout` in your `log4j2.xml`if you're using `log4j2`.

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
   status="warn" strict="true"
   packages="com.sap.hcp.cf.log4j2.converter,com.sap.hcp.cf.log4j2.layout">
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

## Custom Metrics

With the custom metrics feature you can send metrics defined inside your code. Metrics are emitted as log messages when your application is bound to a service called *application-logs*. This is done because of the
special format supported by the SAP BTP Application Logging Service and for compatibility with the prior apporach. To use the feature you'd need:

1. *Instrumenting Spring Boot 2 applications:*

``` xml
<dependency>
  <groupId>com.sap.hcp.cf.logging</groupId>
  <artifactId>cf-custom-metrics-clients-spring-boot</artifactId>
  <version>${cf-logging-version}</version>
</dependency>
```

The Spring Boot instrumentation uses `Spring Boot Actuator` which allows to read predefined metrics and write custom metrics. The Actuator supports [Micrometer](https://github.com/micrometer-metrics/micrometer) and is part of Actuator's dependencies.
In your code you work directly with `Micrometer`. Define your custom metrics and iterate with them:

``` java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

@RestController
public class DemoController {

	private Counter counter;
	private AtomicInteger concurrentHttpRequests;
	private LongTaskTimer longTimer;

	DemoController() {
		this.counter = Metrics.counter("demo.controller.number.of.requests", "unit", "requests");
		List<Tag> tags = new ArrayList<Tag>(Arrays.asList(new Tag[] { Tag.of("parallel", "clients") }));
		this.concurrentHttpRequests = Metrics.gauge("demo.controller.number.of.clients.being.served", tags,
				new AtomicInteger(0));
		this.longTimer = Metrics.more().longTaskTimer("demo.controller.time.spends.in.serving.clients");
	}

	@RequestMapping("/")
	public String index() {
		longTimer.record(() -> {
			this.counter.increment();
			concurrentHttpRequests.addAndGet(1);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.error(e);
			} finally {
				concurrentHttpRequests.addAndGet(-1);
			}
		});

		return "Greetings from Custom Metrics!";
	}
}
```

In the example above, three custom metrics are defined and used. The metrics are `Counter`, `LongTaskTimer` and `Gauge`.

2. *Instrumenting plain Java applications:*

``` xml
<dependency>
  <groupId>com.sap.hcp.cf.logging</groupId>
  <artifactId>cf-custom-metrics-clients-java</artifactId>
  <version>${cf-logging-version}</version>
</dependency>
```

The Java instrumentation uses [Dropwizard](https://metrics.dropwizard.io) which allows to define all kind of metrics supports by Dropwizard. The following metrics are available: `com.codahale.metrics.Gauge`, `com.codahale.metrics.Counter`, `com.codahale.metrics.Histogram`, `com.codahale.metrics.Meter` and `com.codahale.metrics.Timer`. More information about the [metric types and their usage](https://metrics.dropwizard.io/4.0.0/getting-started.html). Define your custom metrics and iterate with them:

``` java
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.sap.cloud.cf.monitoring.java.CustomMetricRegistry;

public class CustomMetricsServlet extends HttpServlet {
    private static Counter counter = CustomMetricRegistry.get().counter("custom.metric.request.count");
    private static Meter meter = CustomMetricRegistry.get().meter("custom.metric.request.meter");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        counter.inc(3);
        meter.mark();
        response.getWriter().println("<p>Greetings from Custom Metrics</p>");
    }
}
```

## Custom metrics Configurations

This library supports the following configurations regarding sending custom metrics:
  * `interval`: the interval for sending metrics, in millis. **Default value: `60000`**
  * `enabled`: enables or disables the sending of metrics. **Default value: `true`**
  * `metrics`: array of whitelisted metric names. Only mentioned metrics would be processed and sent. If it is an empty array all metrics are being sent. **Default value: `[]`**
  * `metricQuantiles`: enables or disables the sending of metric's [quantiles](https://en.wikipedia.org/wiki/Quantile) like median, 95th percentile, 99th percentile. SpringBoot does not support this configuration. **Default value: `false`**

  Configurations are read from environment variable named `CUSTOM_METRICS`. To change the default values, you should override the environment variable with your custom values. Example:

```
{
    "interval": 30000,
    "enabled": true,
    "metrics": [
        "my.whitelist.metric.1",
        "my.whitelist.metric.2"
    ],
    "metricQuantiles":true
}
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

In order to illustrate how the different features are used, this repository includes two sample applications:
  * a Jersey implementation in the  [./sample folder](./sample)
  * a Spring Boot implementation in the [./sample-spring-boot folder](./sample-spring-boot)

## Documentation

More info on the actual implementation can be found in the [Wiki](https://github.com/SAP/cf-java-logging-support/wiki).

## Licensing

Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available via the [REUSE](https://api.reuse.software/info/github.com/SAP/cf-java-logging-support) tool.
