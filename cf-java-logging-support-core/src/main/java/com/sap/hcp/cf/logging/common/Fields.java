package com.sap.hcp.cf.logging.common;

/**
 * JSON field name constants used in structured log messages emitted by this library.
 *
 * <p>Two message types are defined:
 * <ul>
 *   <li><b>log</b> – an application log message ({@link #TYPE} = {@code "log"})</li>
 *   <li><b>request</b> – a request-metrics message ({@link #TYPE} = {@code "request"})</li>
 * </ul>
 *
 * <p>Fields marked <em>shared</em> appear in both message types.
 * Fields marked <em>log</em> are specific to application log messages.
 * Fields marked <em>request</em> are specific to request-metrics messages.
 *
 * <p>Field name conventions:
 * <ul>
 *   <li>Words are separated by {@code _} (no camelCase)</li>
 *   <li>Suffix {@code _ms} → milliseconds</li>
 *   <li>Suffix {@code _b}  → bytes</li>
 *   <li>Suffix {@code _at} → human-readable date/time</li>
 *   <li>Suffix {@code _ts} → nanosecond timestamp</li>
 * </ul>
 */
public interface Fields {

    // -------------------------------------------------------------------------
    // Shared context fields (present in both log and request messages)
    // -------------------------------------------------------------------------

    /**
     * Human-readable date when the message was written.
     * <p>Type: date &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     */
    String WRITTEN_AT = "written_at";

    /**
     * Timestamp in nanosecond precision when the message was written.
     * <p>Type: long &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code 1456820553816849408}
     */
    String WRITTEN_TS = "written_ts";

    /**
     * Unique identifier used to correlate multiple messages into a logical unit (e.g. a business transaction). Uses
     * UUID format. {@code "-"} indicates no value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "db2d002e-2702-41ec-66f5-c002a80a3d3f"}
     */
    String CORRELATION_ID = "correlation_id";

    /**
     * Unique identifier used to correlate multiple messages to a single request.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     * <p>Example: {@code "e24a5963-95eb-4568-b1ae-81b67c41e99a"}
     */
    String REQUEST_ID = "request_id";

    /**
     * Content of the W3C {@code traceparent} header as defined in
     * <a href="https://www.w3.org/TR/trace-context/#traceparent-header">W3C Trace Context</a>.
     * Allows correlation of logs to a distributed trace.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     * <p>Example: {@code "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"}
     */
    String W3C_TRACEPARENT = "w3c_traceparent";

    /**
     * SAP Passport – an end-to-end tracing token used in many SAP products, encoded as a hex string. Applications may
     * include the full passport here or split it into the individual {@code sap_passport_*} fields.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     */
    String SAP_PASSPORT = "sap_passport";

    /** Action field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_ACTION = "sap_passport_Action";

    /** Action type field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_ACTIONTYPE = "sap_passport_ActionType";

    /** Client number field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_CLIENTNUMBER = "sap_passport_ClientNumber";

    /** Connection counter field of an SAP Passport. Type: integer | Optional | Shared */
    String SAP_PASSPORT_CONNECTIONCOUNTER = "sap_passport_ConnectionCounter";

    /** Connection ID field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_CONNECTIONID = "sap_passport_ConnectionId";

    /** Component name field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_COMPONENTNAME = "sap_passport_ComponentName";

    /** Component type field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_COMPONENTTYPE = "sap_passport_ComponentType";

    /** Previous component name field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_PREVIOUSCOMPONENTNAME = "sap_passport_PreviousComponentName";

    /** Trace flags field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_TRACEFLAGS = "sap_passport_TraceFlags";

    /** Transaction ID field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_TRANSACTIONID = "sap_passport_TransactionId";

    /** Root context ID field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_ROOTCONTEXTID = "sap_passport_RootContextId";

    /** User ID field of an SAP Passport. Type: string | Optional | Shared */
    String SAP_PASSPORT_USERID = "sap_passport_UserId";

    /**
     * Unique identifier for the tenant associated with the current request.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     * <p>Example: {@code "e24a5963-95eb-4568-b1ae-81b67c41e99a"}
     */
    String TENANT_ID = "tenant_id";

    /**
     * Subdomain of the current tenant, e.g. {@code "acme-inc"} in {@code https://acme-inc.eu10.cloud.alm.sap/home}.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     */
    String TENANT_SUBDOMAIN = "tenant_subdomain";

    /**
     * Unique identifier of the software component (application or service) that wrote the message. Does not uniquely
     * identify a running instance. In CF, this is the {@code application_id}.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "9e6f3ecf-def0-4baf-8fac-9339e61d5645"}
     */
    String COMPONENT_ID = "component_id";

    /**
     * Human-friendly name of the software component. Not necessarily unique.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     * <p>Example: {@code "my-fancy-component"}
     */
    String COMPONENT_NAME = "component_name";

    /**
     * Type of the software component. Either {@code "application"} or {@code "service"}.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     */
    String COMPONENT_TYPE = "component_type";

    /**
     * Index of the running instance of the component. Defaults to {@code "0"}. In CF, this is the horizontal scale-out
     * instance index.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "7"}
     */
    String COMPONENT_INSTANCE = "component_instance";

    /**
     * Unique identifier of the container running the component instance. An IP address is acceptable when privacy is
     * not a concern. In CF, the {@code INSTANCE_IP} environment variable is used.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Shared
     */
    String CONTAINER_ID = "container_id";

    /**
     * Unique identifier of the Cloud Foundry organization the component belongs to. {@code "-"} indicates no proper
     * value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "280437b3-dd8b-40b1-bbab-1f05a44345f8"}
     */
    String ORGANIZATION_ID = "organization_id";

    /**
     * Human-readable name of the Cloud Foundry organization. Not necessarily unique. {@code "-"} indicates no proper
     * value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "acme"}
     */
    String ORGANIZATION_NAME = "organization_name";

    /**
     * Unique identifier of the Cloud Foundry space the component belongs to. {@code "-"} indicates no proper value
     * could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "280437b3-dd8b-40b1-bbab-1f05a44345f8"}
     */
    String SPACE_ID = "space_id";

    /**
     * Human-readable name of the Cloud Foundry space. Not necessarily unique.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example: {@code "test"}
     */
    String SPACE_NAME = "space_name";

    /**
     * The execution layer or processing component that emitted the message.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     * <p>Example (log): {@code "[JAVA:BusinessLogicController]"} &nbsp; Example (request): {@code "[CF/RTR]"}
     */
    String LAYER = "layer";

    /**
     * Message type tag making the message self-contained. {@code "log"} for application log messages, {@code "request"}
     * for request-metrics messages.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Shared
     */
    String TYPE = "type";

    // -------------------------------------------------------------------------
    // Application log fields (TYPE = "log")
    // -------------------------------------------------------------------------

    /**
     * The logger name that produced the message, typically the fully-qualified Java class name.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Log
     * <p>Example: {@code "com.sap.demo.shine.OrderController"}
     */
    String LOGGER = "logger";

    /**
     * Name of the thread that wrote the log message. {@code "-"} if unavailable.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Log
     * <p>Example: {@code "http-nio-4655"}
     */
    String THREAD = "thread";

    /**
     * Log severity level.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Log
     * <p>Example: {@code "INFO"}
     */
    String LEVEL = "level";

    /**
     * The original log message written by the application.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Log
     * <p>Example: {@code "This is a log message"}
     */
    String MSG = "msg";

    /**
     * Java class name of the logged exception, when available.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Log
     * <p>Example: {@code "java.lang.NullPointerException"}
     */
    String EXCEPTION_TYPE = "exception_type";

    /**
     * Message of the logged exception, when available.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Log
     * <p>Example: {@code "Something went wrong"}
     */
    String EXCEPTION_MESSAGE = "exception_message";

    /**
     * Stacktrace of a logged exception. Must not contain newline or tab characters.
     * <p>Type: array &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Log
     */
    String STACKTRACE = "stacktrace";

    /**
     * List of names to further categorize this log message.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Log
     */
    String CATEGORIES = "categories";

    /**
     * Container for non-standard custom fields. The {@code "string"} sub-field holds entries with key {@code "k"},
     * value {@code "v"}, and index {@code "i"}.
     * <p>Type: object &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Log
     * <p>JSON key: {@code "#cf"}
     */
    String CUSTOM_FIELDS = "#cf";

    // -------------------------------------------------------------------------
    // Request-metrics fields (TYPE = "request")
    // -------------------------------------------------------------------------

    /**
     * The request path or command that was processed. {@code "-"} indicates no proper value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Request
     * <p>Example: {@code "/get/api/v2"}
     */
    String REQUEST = "request";

    /**
     * Human-readable date when the request was sent to the processing component (millisecond precision, UTC). Not
     * available at the producer end unless shipped with the request headers.
     * <p>Type: date &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String REQUEST_SENT_AT = "request_sent_at";

    /**
     * Human-readable date when the request was received by the processing component (millisecond precision, UTC). Not
     * available at the consumer end unless shipped with the response headers.
     * <p>Type: date &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String REQUEST_RECEIVED_AT = "request_received_at";

    /**
     * Human-readable date when the response was sent back by the processing component (millisecond precision, UTC).
     * <p>Type: date &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String RESPONSE_SENT_AT = "response_sent_at";

    /**
     * Human-readable date when the response was received by the requesting component (millisecond precision, UTC).
     * <p>Type: date &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String RESPONSE_RECEIVED_AT = "response_received_at";

    /**
     * Direction of the request. {@code "IN"} for incoming, {@code "OUT"} for outgoing. Default is {@code "IN"}.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Request
     */
    String DIRECTION = "direction";

    /**
     * Time in milliseconds taken by the processing component to compute the response. For consumer-side outgoing
     * requests, this is the total wait time until the response arrived. {@code -1} indicates the value could not be
     * measured.
     * <p>Type: float &nbsp;|&nbsp; Required &nbsp;|&nbsp; Request
     * <p>Example: {@code 43.476}
     */
    String RESPONSE_TIME_MS = "response_time_ms";

    /**
     * Technical protocol used for the request, e.g. {@code "HTTP/1.1"} or {@code "JDBC/1.2"}. {@code "-"} indicates no
     * proper value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Request
     */
    String PROTOCOL = "protocol";

    /**
     * Protocol method of the request, e.g. {@code "GET"}. {@code "-"} indicates no proper value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Request
     */
    String METHOD = "method";

    /**
     * IP address of the consumer (incoming) or remote producer (outgoing). {@code "-"} indicates no proper value could
     * be provided.
     * <p>Type: string &nbsp;|&nbsp; Required &nbsp;|&nbsp; Request
     * <p>Example: {@code "192.168.0.1"}
     */
    String REMOTE_IP = "remote_ip";

    /**
     * Hostname of the consumer (incoming) or remote producer (outgoing). {@code "-"} indicates no proper value could be
     * provided.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String REMOTE_HOST = "remote_host";

    /**
     * Port number on which the request connection was established. Typed as string.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "1234"}
     */
    String REMOTE_PORT = "remote_port";

    /**
     * Username associated with the request. Empty if the request is not authenticated.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "d058433"}
     */
    String REMOTE_USER = "remote_user";

    /**
     * Size of the request entity in bytes. {@code -1} if the request contains no entity.
     * <p>Type: long &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String REQUEST_SIZE_B = "request_size_b";

    /**
     * Numeric HTTP (or protocol) status code of the response. {@code -1} indicates the value could not be determined.
     * <p>Type: integer &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code 200}
     */
    String RESPONSE_STATUS = "response_status";

    /**
     * Size of the response entity in bytes. {@code -1} if the response contains no entity.
     * <p>Type: long &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String RESPONSE_SIZE_B = "response_size_b";

    /**
     * MIME type of the response entity. {@code "-"} indicates no proper value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "application/json"}
     */
    String RESPONSE_CONTENT_TYPE = "response_content_type";

    /**
     * Address from which the request originated. {@code "-"} indicates no proper value could be provided.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "/index.html"}
     */
    String REFERER = "referer";

    /**
     * Comma-separated list of IP addresses from the {@code X-Forwarded-For} header, leftmost being the original
     * client.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "192.0.2.60,10.12.9.23"}
     */
    String X_FORWARDED_FOR = "x_forwarded_for";

    /**
     * Original host requested by the client, forwarded by a proxy via the {@code X-Forwarded-Host} header.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "requested-host.example.com"}
     */
    String X_FORWARDED_HOST = "x_forwarded_host";

    /**
     * Original protocol used by the client before the proxy, forwarded via the {@code X-Forwarded-Proto} header.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "https"}
     */
    String X_FORWARDED_PROTO = "x_forwarded_proto";

    /**
     * Custom host header set by a proxy or load balancer for special use cases.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "central-host.example.com"}
     */
    String X_CUSTOM_HOST = "x_custom_host";

    /**
     * HA-Proxy header indicating whether the client used a secured connection: {@code "1"} (yes) or {@code "0"} (no).
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String X_SSL_CLIENT = "x_ssl_client";

    /**
     * HA-Proxy header indicating the TLS/SSL connection status code.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String X_SSL_CLIENT_VERIFY = "x_ssl_client_verify";

    /**
     * HA-Proxy header providing the full distinguished name of the client certificate.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "/C=FR/ST=.../CN=client1/emailAddress=ba@haproxy.com"}
     */
    String X_SSL_CLIENT_SUBJECT_DN = "x_ssl_client_subject_dn";

    /**
     * HA-Proxy header providing the common name of the client certificate.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "client1"}
     */
    String X_SSL_CLIENT_SUBJECT_CN = "x_ssl_client_subject_cn";

    /**
     * HA-Proxy header providing the full distinguished name of the certificate issuer.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String X_SSL_CLIENT_ISSUER_DN = "x_ssl_client_issuer_dn";

    /**
     * HA-Proxy header providing the certificate start date as {@code YYMMDDhhmmss}.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "130613144555Z"}
     */
    String X_SSL_CLIENT_NOTBEFORE = "x_ssl_client_notbefore";

    /**
     * HA-Proxy header providing the certificate end date as {@code YYMMDDhhmmss}.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     * <p>Example: {@code "140613144555Z"}
     */
    String X_SSL_CLIENT_NOTAFTER = "x_ssl_client_notafter";

    /**
     * SSL client session identifier.
     * <p>Type: string &nbsp;|&nbsp; Optional &nbsp;|&nbsp; Request
     */
    String X_SSL_CLIENT_SESSION_ID = "x_ssl_client_session_id";
}
