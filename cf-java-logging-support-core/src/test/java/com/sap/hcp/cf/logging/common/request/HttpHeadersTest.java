package com.sap.hcp.cf.logging.common.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;

public class HttpHeadersTest {

    @Before
    public void resetLogContext() {
        LogContext.resetContextFields();
        HttpHeaders.propagated().stream().map(HttpHeader::getField).forEach(LogContext::remove);
    }

    @Test
    public void hasCorrectNumberOfTypes() throws Exception {
        assertThat(HttpHeaders.values().length, is(equalTo(19)));
    }

    @Test
    public void hasCorrectNames() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getName(), is("content-length"));
        assertThat(HttpHeaders.CONTENT_TYPE.getName(), is("content-type"));
        assertThat(HttpHeaders.CORRELATION_ID.getName(), is("X-CorrelationID"));
        assertThat(HttpHeaders.REFERER.getName(), is("referer"));
        assertThat(HttpHeaders.TENANT_ID.getName(), is("tenantid"));
        assertThat(HttpHeaders.X_CUSTOM_HOST.getName(), is("x-custom-host"));
        assertThat(HttpHeaders.X_FORWARDED_FOR.getName(), is("x-forwarded-for"));
        assertThat(HttpHeaders.X_FORWARDED_HOST.getName(), is("x-forwarded-host"));
        assertThat(HttpHeaders.X_FORWARDED_PROTO.getName(), is("x-forwarded-proto"));
        assertThat(HttpHeaders.X_VCAP_REQUEST_ID.getName(), is("x-vcap-request-id"));
        assertThat(HttpHeaders.X_SSL_CLIENT.getName(), is("x-ssl-client"));
        assertThat(HttpHeaders.X_SSL_CLIENT_VERIFY.getName(), is("x-ssl-client-verify"));
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN.getName(), is("x-ssl-client-subject-dn"));
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN.getName(), is("x-ssl-client-subject-cn"));
        assertThat(HttpHeaders.X_SSL_CLIENT_ISSUER_DN.getName(), is("x-ssl-client-issuer-dn"));
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTBEFORE.getName(), is("x-ssl-client-notbefore"));
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTAFTER.getName(), is("x-ssl-client-notafter"));
        assertThat(HttpHeaders.X_SSL_CLIENT_SESSION_ID.getName(), is("x-ssl-client-session-id"));
    }

    @Test
    public void hasCorrectFields() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getField(), is(nullValue()));
        assertThat(HttpHeaders.CONTENT_TYPE.getField(), is(nullValue()));
        assertThat(HttpHeaders.CORRELATION_ID.getField(), is(Fields.CORRELATION_ID));
        assertThat(HttpHeaders.REFERER.getField(), is(nullValue()));
        assertThat(HttpHeaders.TENANT_ID.getField(), is(Fields.TENANT_ID));
        assertThat(HttpHeaders.X_CUSTOM_HOST.getField(), is(Fields.X_CUSTOM_HOST));
        assertThat(HttpHeaders.X_FORWARDED_FOR.getField(), is(Fields.X_FORWARDED_FOR));
        assertThat(HttpHeaders.X_FORWARDED_HOST.getField(), is(Fields.X_FORWARDED_HOST));
        assertThat(HttpHeaders.X_FORWARDED_PROTO.getField(), is(Fields.X_FORWARDED_PROTO));
        assertThat(HttpHeaders.X_VCAP_REQUEST_ID.getField(), is(Fields.REQUEST_ID));
        assertThat(HttpHeaders.X_SSL_CLIENT.getField(), is(Fields.X_SSL_CLIENT));
        assertThat(HttpHeaders.X_SSL_CLIENT_VERIFY.getField(), is(Fields.X_SSL_CLIENT_VERIFY));
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN.getField(), is(Fields.X_SSL_CLIENT_SUBJECT_DN));
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN.getField(), is(Fields.X_SSL_CLIENT_SUBJECT_CN));
        assertThat(HttpHeaders.X_SSL_CLIENT_ISSUER_DN.getField(), is(Fields.X_SSL_CLIENT_ISSUER_DN));
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTBEFORE.getField(), is(Fields.X_SSL_CLIENT_NOTBEFORE));
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTAFTER.getField(), is(Fields.X_SSL_CLIENT_NOTAFTER));
        assertThat(HttpHeaders.X_SSL_CLIENT_SESSION_ID.getField(), is(Fields.X_SSL_CLIENT_SESSION_ID));
    }

    @Test
    public void defaultFieldValueIsUnknownWithoutConfiguredField() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getFieldValue(), is(Defaults.UNKNOWN));
        assertThat(HttpHeaders.CONTENT_TYPE.getFieldValue(), is(Defaults.UNKNOWN));
        assertThat(HttpHeaders.REFERER.getFieldValue(), is(Defaults.UNKNOWN));
    }

    @Test
    public void defaultFieldValueIsNullForProgatedHeaders() throws Exception {
        for (HttpHeader header: HttpHeaders.propagated()) {
            assertThat("Default of field <" + header.getField() + "> from header <" + header.getName() +
                       "> should be null", header.getFieldValue(), is(nullValue()));
        }
    }

    @Test
    public void hasCorrectAliases() throws Exception {
        assertThat(HttpHeaders.CONTENT_LENGTH.getAliases(), is(empty()));
        assertThat(HttpHeaders.CONTENT_TYPE.getAliases(), is(empty()));
        assertThat(HttpHeaders.CORRELATION_ID.getAliases(), containsInAnyOrder(HttpHeaders.X_VCAP_REQUEST_ID));
        assertThat(HttpHeaders.REFERER.getAliases(), is(empty()));
        assertThat(HttpHeaders.TENANT_ID.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_CUSTOM_HOST.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_FORWARDED_FOR.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_FORWARDED_HOST.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_FORWARDED_PROTO.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_VCAP_REQUEST_ID.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_VERIFY.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_DN.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_SUBJECT_CN.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_ISSUER_DN.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTBEFORE.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_NOTAFTER.getAliases(), is(empty()));
        assertThat(HttpHeaders.X_SSL_CLIENT_SESSION_ID.getAliases(), is(empty()));
    }

    @Test
    public void propagatesCorrectHeaders() throws Exception {
        assertThat(HttpHeaders.propagated(), containsInAnyOrder(HttpHeaders.CORRELATION_ID, HttpHeaders.SAP_PASSPORT,
                                                                HttpHeaders.TENANT_ID, HttpHeaders.X_VCAP_REQUEST_ID));
    }

}
