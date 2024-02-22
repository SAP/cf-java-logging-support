package com.sap.hcp.cf.logging.common.request;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpHeadersTest {

    @BeforeEach
    public void resetLogContext() {
        LogContext.resetContextFields();
        HttpHeaders.propagated().stream().map(HttpHeader::getField).forEach(LogContext::remove);
    }

    @Test
    public void hasCorrectNumberOfTypes() throws Exception {
        assertThat(HttpHeaders.values()).hasSize(20);
    }

    @Test
    public void hasCorrectNames() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getName()).isEqualTo("content-length");
        assertThat(HttpHeaders.CONTENT_TYPE.getName()).isEqualTo("content-type");
        assertThat(HttpHeaders.CORRELATION_ID.getName()).isEqualTo("X-CorrelationID");
        assertThat(HttpHeaders.REFERER.getName()).isEqualTo("referer");
        assertThat(HttpHeaders.TENANT_ID.getName()).isEqualTo("tenantid");
        assertThat(HttpHeaders.W3C_TRACEPARENT.getName()).isEqualTo("traceparent");
        assertThat(HttpHeaders.X_CUSTOM_HOST.getName()).isEqualTo("x-custom-host");
        assertThat(HttpHeaders.X_FORWARDED_FOR.getName()).isEqualTo("x-forwarded-for");
        assertThat(HttpHeaders.X_FORWARDED_HOST.getName()).isEqualTo("x-forwarded-host");
        assertThat(HttpHeaders.X_FORWARDED_PROTO.getName()).isEqualTo("x-forwarded-proto");
        assertThat(HttpHeaders.X_VCAP_REQUEST_ID.getName()).isEqualTo("x-vcap-request-id");
        assertThat(HttpHeaders.X_SSL_CLIENT.getName()).isEqualTo("x-ssl-client");
        assertThat(HttpHeaders.X_SSL_CLIENT_VERIFY.getName()).isEqualTo("x-ssl-client-verify");
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN.getName()).isEqualTo("x-ssl-client-subject-dn");
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN.getName()).isEqualTo("x-ssl-client-subject-cn");
        assertThat(HttpHeaders.X_SSL_CLIENT_ISSUER_DN.getName()).isEqualTo("x-ssl-client-issuer-dn");
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTBEFORE.getName()).isEqualTo("x-ssl-client-notbefore");
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTAFTER.getName()).isEqualTo("x-ssl-client-notafter");
        assertThat(HttpHeaders.X_SSL_CLIENT_SESSION_ID.getName()).isEqualTo("x-ssl-client-session-id");
    }

    @Test
    public void hasCorrectFields() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getField()).isNull();
        assertThat(HttpHeaders.CONTENT_TYPE.getField()).isNull();
        assertThat(HttpHeaders.CORRELATION_ID.getField()).isEqualTo(Fields.CORRELATION_ID);
        assertThat(HttpHeaders.REFERER.getField()).isNull();
        assertThat(HttpHeaders.TENANT_ID.getField()).isEqualTo(Fields.TENANT_ID);
        assertThat(HttpHeaders.W3C_TRACEPARENT.getField()).isEqualTo(Fields.W3C_TRACEPARENT);
        assertThat(HttpHeaders.X_CUSTOM_HOST.getField()).isEqualTo(Fields.X_CUSTOM_HOST);
        assertThat(HttpHeaders.X_FORWARDED_FOR.getField()).isEqualTo(Fields.X_FORWARDED_FOR);
        assertThat(HttpHeaders.X_FORWARDED_HOST.getField()).isEqualTo(Fields.X_FORWARDED_HOST);
        assertThat(HttpHeaders.X_FORWARDED_PROTO.getField()).isEqualTo(Fields.X_FORWARDED_PROTO);
        assertThat(HttpHeaders.X_VCAP_REQUEST_ID.getField()).isEqualTo(Fields.REQUEST_ID);
        assertThat(HttpHeaders.X_SSL_CLIENT.getField()).isEqualTo(Fields.X_SSL_CLIENT);
        assertThat(HttpHeaders.X_SSL_CLIENT_VERIFY.getField()).isEqualTo(Fields.X_SSL_CLIENT_VERIFY);
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN.getField()).isEqualTo(Fields.X_SSL_CLIENT_SUBJECT_DN);
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN.getField()).isEqualTo(Fields.X_SSL_CLIENT_SUBJECT_CN);
        assertThat(HttpHeaders.X_SSL_CLIENT_ISSUER_DN.getField()).isEqualTo(Fields.X_SSL_CLIENT_ISSUER_DN);
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTBEFORE.getField()).isEqualTo(Fields.X_SSL_CLIENT_NOTBEFORE);
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTAFTER.getField()).isEqualTo(Fields.X_SSL_CLIENT_NOTAFTER);
        assertThat(HttpHeaders.X_SSL_CLIENT_SESSION_ID.getField()).isEqualTo(Fields.X_SSL_CLIENT_SESSION_ID);
    }

    @Test
    public void defaultFieldValueIsUnknownWithoutConfiguredField() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getFieldValue()).isEqualTo(Defaults.UNKNOWN);
        assertThat(HttpHeaders.CONTENT_TYPE.getFieldValue()).isEqualTo(Defaults.UNKNOWN);
        assertThat(HttpHeaders.REFERER.getFieldValue()).isEqualTo(Defaults.UNKNOWN);
    }

    @Test
    public void defaultFieldValueIsNullForProgatedHeaders() throws Exception {
        for (HttpHeader header: HttpHeaders.propagated()) {
            assertThat(header.getFieldValue()).describedAs(
                                                      "Default of field <" + header.getField() + "> from header <" + header.getName() + "> should be null")
                                              .isNull();
        }
    }

    @Test
    public void hasCorrectAliases() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getAliases()).isEmpty();
        assertThat(HttpHeaders.CONTENT_TYPE.getAliases()).isEmpty();
        assertThat(HttpHeaders.CORRELATION_ID.getAliases()).containsExactly(HttpHeaders.X_VCAP_REQUEST_ID);
        assertThat(HttpHeaders.REFERER.getAliases()).isEmpty();
        assertThat(HttpHeaders.TENANT_ID.getAliases()).isEmpty();
        assertThat(HttpHeaders.W3C_TRACEPARENT.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_CUSTOM_HOST.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_FORWARDED_FOR.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_FORWARDED_HOST.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_FORWARDED_PROTO.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_VCAP_REQUEST_ID.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_VERIFY.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_ISSUER_DN.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTBEFORE.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTAFTER.getAliases()).isEmpty();
        assertThat(HttpHeaders.X_SSL_CLIENT_SESSION_ID.getAliases()).isEmpty();
    }

    @Test
    public void propagatesCorrectHeaders() throws Exception {
        assertThat(HttpHeaders.propagated()).contains(HttpHeaders.CORRELATION_ID, HttpHeaders.SAP_PASSPORT,
                                                      HttpHeaders.TENANT_ID, HttpHeaders.W3C_TRACEPARENT,
                                                      HttpHeaders.X_VCAP_REQUEST_ID);
    }

}
