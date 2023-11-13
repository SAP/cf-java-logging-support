# Spring Boot Sample Application

This sample application shows how cf-java-logging-support can be used in an application. 
It features a small Spring Boot application to showcase various features provided by the library. The application provides a REST API to trigger several actions.
See section [Features](#features) for details. 
You can deploy the sample application just with the library implementation using the default [manifest.yml](manifest.yml).
Alternatively, you can choose to add instrumentation with the OpenTelemetry Java Agent and the [Java Agent Extension](../cf-java-logging-support-opentelemetry-agent-extension) provided by this library. 
See the section [Adding the OpenTelemetry Java Agent](#adding-the-opentelemetry-java-agent) for details.

## Getting Started

This sample application is contained in the Maven module sample-spring-boot. 
It can be built with a simple `mvn install`.
A minimal CF application manifest is provided in [manifest.yml.](manifest.yml)
This allows to deploy the newly built app with `cf push`, provided a login to a CF instance.

### Changing the Credentials

This sample application contains several credentials to secure the access.
The credentials should be changed, before the application is built and deployed.

#### Basic Authentication

Every Rest endpoint is secured with Basic Authentication.
The corresponding username and password are contained in the [application.yml](src/main/resources/application.yml).
Please change the values of `username` and `password` in the following section of that file.

```yaml
auth:
  basic:
    username: user
    password: secret
```

#### Keystore Configuration

This sample application uses a JKS keystore.
It is created by Maven, when it cannot be found in `target/generated-resources/keystore/token_keystore.jks`
The credentials to that keystore as well as the dname for the generated key can be configured as Maven properties in the [pom.xml](pom.xml):

```xml
<properties>
    <!-- other properties -->
	<keystore.token.store_password>0bzhBRNUXBR5</keystore.token.store_password>
	<keystore.token.key_password>0bzhBRNUXBR5</keystore.token.key_password>
	<keystore.token.key_alias>jwt-token</keystore.token.key_alias>
	<keystore.token.dname>CN=cf-java-logging-support, OU=None, O=SAP, L=Unknown, ST=Unknown, C=Unknown</keystore.token.dname>
</properties>
```

**Note:** Creating the keystore with Maven means, that it will be deleted by `mvn clean` and a new key pair will be created with `mvn install`.

### Changing the Logging Backend

This sample application can use both supported logging backends.
By default it will use logback, which is configured by [logback.xml](src/main/resources/logback.xml).

Log4j2 can be chosen as backend by using the corresponding Maven profile:

```bash
mvn install -Plog4j2
```

The configuration file is [log4j2.xml](src/main/resources/log4j2.xml) in that case.

## Features

This sample application offers a Rest API with several endpoints to show different features of cf-java-logging-support.
A summary of the endpoints is given in the following table.
Detailed descriptions are given in later sections.

| Feature | Endpoint | Request Parameter |
|---------|----------|-------------------|
| [Generate Log Message](#generating-log-messages) | `POST /log/{logger}/{logLevel}` <br /><dl><dt>`{logger}`</dt><dd>the logger name, usually a class name</dd><dt>`{logLevel}`</dt><dd>the log level, one of `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`</dd></dl>| <dl><dt>`m=message`_(optional)_</dt><dd>the message to log</dd></dl> |
| [Create JWT for dynamic log level](#creating-tokens) | `GET /token/{logLevel}` <br /><dl><dt>`{logLevel}`</dt><dd>the dynamic log level, one of `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`</dd></dl>| <dl><dt>`p=packagenames` _(optional)_</dt><dd>a comma-separated list of package names</dd><dt>`exp=unixEpoch` _(optional)_</dt><dd>the expiry time in milliseconds since Unix epoch</dd></dl>|
| [Get public key for JWT verification](#get-public-key) | `GET /publickey` ||

### Generating Log Messages

This sample application allows the generation of log messages triggered by an HTTP request.
Users can send a POST request containing the logger, the required log level and optionally the message to log.
This allows testing the logging configuration with respect to packages and logging levels.

**Example:**

```bash
$ curl -X POST -u user:secret localhost:8080/log/test/info
Generated info log with message: "This is the default log message!".
```

```json
{ 
    "written_at":"2021-02-13T10:25:18.673Z",
    "written_ts":1613211918673355000,
    "tenant_id":"-",
    "component_id":"-",
    "component_name":"-",
    "organization_name":"-",
    "component_type":"application",
    "space_name":"-",
    "component_instance":"0",
    "organization_id":"-",
    "correlation_id":"81c759fd-4433-4d06-bddf-c5c30199c49b",
    "space_id":"-",
    "container_id":"-",
    "tenant_subdomain":"-",
    "type":"log",
    "logger":"test",
    "thread":"http-nio-8080-exec-2",
    "level":"INFO",
    "categories":[],
    "msg":"This is the default log message!" 
}
{ 
    "written_at":"2021-02-13T10:25:18.676Z",
    "written_ts":1613211918677807000,
    "tenant_id":"-",
    "component_id":"-",
    "component_name":"-",
    "organization_name":"-",
    "component_type":"application",
    "space_name":"-",
    "component_instance":"0",
    "organization_id":"-",
    "correlation_id":"81c759fd-4433-4d06-bddf-c5c30199c49b",
    "space_id":"-",
    "container_id":"-",
    "tenant_subdomain":"-",
    "type":"request",
    "request":"/log/test/info",
    "referer":"-",
    "response_sent_at":"2021-02-13T10:25:18.676Z",
    "response_status":200,
    "method":"POST",
    "response_size_b":68,
    "request_size_b":-1,
    "remote_port":"redacted",
    "layer":"[SERVLET]",
    "remote_host":"redacted",
    "x_forwarded_for":"-",
    "remote_user":"redacted",
    "protocol":"HTTP/1.1",
    "remote_ip":"redacted",
    "response_content_type":"text/plain;charset=ISO-8859-1",
    "request_received_at":"2021-02-13T10:25:18.665Z",
    "response_time_ms":10.78147,
    "direction":"IN"
}
```

Note, that the request generates the default log message, that is also contained in the HTTP response and additionally a request log.
Since this example was executed locally, the CF metadata is empty.

A custom message can be given as well:

```bash
$ curl -X POST -u user:secret 'localhost:8080/log/test/info?m=Hello+cf-java-logging-support!'
Generated info log with message: "Hello cf-java-logging-support!".
```

Note the quotes for bash and the HTTP encoding of the whitespace in the message.

### Dynamic Log Levels

This sample-application supports dynamic log levels with package support. 
This feature is documented in detail in the [project wiki.](https://github.com/SAP/cf-java-logging-support/wiki/Dynamic-Log-Levels)
It allows to change the log level by sending the HTTP header `SAP_LOG_LEVEL` with an appropriate JWT.
Since it is not straight forward to generate such a token, there is an endpoint to create such a JWT.

#### Creating Tokens

The generated token needs to contain three things:

* the log level to apply
* the package names or prefixes to use
* an expiration timestamp

The log level is taken as path variable.
Since package names and expiration timestamp are optional, they can be specified as request parameters.
JWTs to be used with this sample application require a package name, but an empty string is allowed.
If no expiration timestamp is provided the application will take the current timestamp and add the default period as configure in `defaults.token.expiration: P2D` in [application.yml](src/main/resources/application.yml) (two days by default).
You can change the JWT issuer with property `defaults.token.issuer: your-issuer` in [application.yml](src/main/resources/application.yml).

**Example:**

```bash
$ curl -u user:secret 'localhost:8080/token/DEBUG?p=com,org'
eyJraWQiOiJqd3QtdG9rZW4iLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJsZXZlbCI6IkRFQlVHIiwiaXNzIjoic2FtcGxlLWFwcC1zcHJpbmctYm9vdCIsImV4cCI6MTYxMzM4Njg2MCwicGFja2FnZXMiOiJjb20sb3JnIiwiaWF0IjoxNjEzMjE0MDYwfQ.YtTk8VnMGN2i3SRLK8GRCDfgQcnAQL04IqojZdVYwEJFCezJsot20MYN-WpAWizVVV3midunnBrNzR3_dByK2gRzTQGGE9rXh4KpLw_4UM6XUJTgpMU8yzqt4pCBT-wpMbJ8UOKbT2RCdqOU1oWJL6rggxi5hGBItTvu0PZzjSG3Zv1eIvDKZcNF9pq4F1r8H2og1Mun28o1r-J5SCURjmolunKDNp4e6fsGSeUttbT5EulIcfHcM9nD4Byyywc2Khs0H13YPqAjdxxFcu_5fYp8JFgbns2Lo5PYjMbY8nxnuZFJmILwXHHRtAoxrcSbpSzFRtZfQsI4earGBGSyog
```

The response payload decodes to:

```json
{
    "level": "DEBUG",
    "iss": "sample-app-spring-boot",
    "exp": 1613386860,
    "packages": "com,org",
    "iat": 1613214060
}
```

**Note:** cf-java-logging-support requires the log level to be one of `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` in all-caps.


#### Using Tokens

The tokens created with the token endpoint can be used as HTTP headers on all endpoints of the application.
We use the token created in the previous section to post a new log message.
Note, that the package name prefixes `org` and `com` will trigger debug logs from many classes, especially Spring (org.springframework.\*) and this libray (com.sap.\*).

```bash
$ curl -X POST -u user:secret 'localhost:8080/log/test/info?m=Hello+cf-java-logging-support!' -H 'SAP-LOG-LEVEL: eyJraWQiOiJqd3QtdG9rZW4iLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJsZXZlbCI6IkRFQlVHIiwiaXNzIjoic2FtcGxlLWFwcC1zcHJpbmctYm9vdCIsImV4cCI6MTYxMzM4Njg2MCwicGFja2FnZXMiOiJjb20sb3JnIiwiaWF0IjoxNjEzMjE0MDYwfQ.YtTk8VnMGN2i3SRLK8GRCDfgQcnAQL04IqojZdVYwEJFCezJsot20MYN-WpAWizVVV3midunnBrNzR3_dByK2gRzTQGGE9rXh4KpLw_4UM6XUJTgpMU8yzqt4pCBT-wpMbJ8UOKbT2RCdqOU1oWJL6rggxi5hGBItTvu0PZzjSG3Zv1eIvDKZcNF9pq4F1r8H2og1Mun28o1r-J5SCURjmolunKDNp4e6fsGSeUttbT5EulIcfHcM9nD4Byyywc2Khs0H13YPqAjdxxFcu_5fYp8JFgbns2Lo5PYjMbY8nxnuZFJmILwXHHRtAoxrcSbpSzFRtZfQsI4earGBGSyog'
Generated info log with message: "Hello cf-java-logging-support!".
```

```json
{ "written_at":"2021-02-13T11:01:25.914Z","written_ts":1613214085914635000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.web.servlet.DispatcherServlet","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"POST \"/log/test/info?m=Hello+cf-java-logging-support!\", parameters={masked}" }
{ "written_at":"2021-02-13T11:01:25.915Z","written_ts":1613214085915196000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"Mapped to com.sap.hcp.cf.logging.sample.springboot.controller.LogController#generateLog(String, String, String)" }
{ "written_at":"2021-02-13T11:01:25.915Z","written_ts":1613214085915814000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"test","thread":"http-nio-8080-exec-6","level":"INFO","categories":[],"msg":"Hello cf-java-logging-support!" }
{ "written_at":"2021-02-13T11:01:25.916Z","written_ts":1613214085916279000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"Using 'text/plain', given [*/*] and supported [text/plain, */*, application/json, application/*+json]" }
{ "written_at":"2021-02-13T11:01:25.916Z","written_ts":1613214085916702000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"Writing [\"Generated info log with message: \"Hello cf-java-logging-support!\".\"]" }
{ "written_at":"2021-02-13T11:01:25.917Z","written_ts":1613214085917217000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.security.web.header.writers.HstsHeaderWriter","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"Not injecting HSTS header since it did not match the requestMatcher org.springframework.security.web.header.writers.HstsHeaderWriter$SecureRequestMatcher@279e2b53" }
{ "written_at":"2021-02-13T11:01:25.917Z","written_ts":1613214085917528000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.security.web.context.HttpSessionSecurityContextRepository","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"SecurityContext 'org.springframework.security.core.context.SecurityContextImpl@442b5a9f: Authentication: org.springframework.security.authentication.UsernamePasswordAuthenticationToken@442b5a9f: Principal: org.springframework.security.core.userdetails.User@36ebcb: Username: user; Password: [PROTECTED]; Enabled: true; AccountNonExpired: true; credentialsNonExpired: true; AccountNonLocked: true; Granted Authorities: ROLE_USER; Credentials: [PROTECTED]; Authenticated: true; Details: org.springframework.security.web.authentication.WebAuthenticationDetails@b364: RemoteIpAddress: 0:0:0:0:0:0:0:1; SessionId: null; Granted Authorities: ROLE_USER' stored to HttpSession: 'org.apache.catalina.session.StandardSessionFacade@488b143c" }
{ "written_at":"2021-02-13T11:01:25.918Z","written_ts":1613214085918181000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"log","logger":"org.springframework.web.servlet.DispatcherServlet","thread":"http-nio-8080-exec-6","level":"DEBUG","categories":[],"msg":"Completed 200 OK" }
{ "written_at":"2021-02-13T11:01:25.918Z","written_ts":1613214085918645000,"tenant_id":"-","component_id":"-","component_name":"-","organization_name":"-","component_type":"application","space_name":"-","component_instance":"0","organization_id":"-","dynamic_log_level_prefixes":"com,org","correlation_id":"c7a92c5e-1e69-4ef9-98ad-8cca27accab9","space_id":"-","container_id":"-","dynamic_log_level":"DEBUG","tenant_subdomain":"-","type":"request","request":"/log/test/info?m=Hello+cf-java-logging-support!","referer":"-","response_sent_at":"2021-02-13T11:01:25.918Z","response_status":200,"method":"POST","response_size_b":66,"request_size_b":-1,"remote_port":"redacted","layer":"[SERVLET]","remote_host":"redacted","x_forwarded_for":"-","remote_user":"redacted","protocol":"HTTP/1.1","remote_ip":"redacted","response_content_type":"text/plain;charset=ISO-8859-1","request_received_at":"2021-02-13T11:01:25.914Z","response_time_ms":3.90267,"direction":"IN"}
```

As you can see there are now several debug messages from Spring, that are usually suppressed by the logging configuration but are now written due to the JWT configuration.

#### Get Public Key

This sample application can be used as a JWT generator for applications in production.
For the configuration of those application cf-java-logging-support needs the public key to validate the JWT signature.
This public key can be obtained

```bash
$ curl -u user:secret 'localhost:8080/publickey'            
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk/a5ZKvQ0voLi29klo5GCtN40JpscGud5diz2y5mNVCMvU6rdE+yNMs5zfgjy2PhR0TLXWfZrwHeX75dhC49hJup39pClv83FJWVbu6ScWNQUGYgdUY5zGkFcBayZlt1yXybQaCUtC8ksHe+QOAUW9Y43nPa8/vznH+zbROlF/kSHIjegcr0GF6ZOLMBAj+9p6Xp+kZxsFUgnqgIZWUp4YI3+j2xDuBgNptZbjUg7WsqEU/u+CA5uCyjGVriq++qSW1fj1A0C29uj1+n3IqrMlL2MdMQayS/5ppyrjApsSYDG56wQEAOrOKaSeBsZexIvIdhQ12+5SkqwPlQGCUHpQIDAQAB
```

**Note:** If this application is used this way you may want to fix the keystore and not accidentally delete it with `mvn clean`. 
You can add your keystore as `src/main/resoures/token_keystore.jks`, which will override the automatically created keystore.

## Adding the OpenTelemetry Java Agent

The sample application comes bundled with the [OpenTelemetry Java Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/) and the [agent extension](../cf-java-logging-support-opentelemetry-agent-extension/README.md) provided by this library. 
The Java agent allows additional auto-instrumentation for traces and metrics.
It can also tap into logback or log4j to forward the logs via OpenTelemetry.
The example [manifest-otel-javaagent.yml](./manifest-otel-javaagent.yml) shows, how to deploy this sample application with OpenTelemetry support enabled.
Note, that the extension scans the application bindings for SAP Cloud Logging as the OpenTelemetry sink.
The application needs to be bound to such a service instance.
