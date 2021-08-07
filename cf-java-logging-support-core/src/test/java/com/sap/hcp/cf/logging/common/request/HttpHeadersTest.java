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
        assertThat(HttpHeaders.values().length, is(equalTo(11)));
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
    }

    @Test
    public void propagatesCorrectHeaders() throws Exception {
        assertThat(HttpHeaders.propagated(), containsInAnyOrder(HttpHeaders.CORRELATION_ID, HttpHeaders.SAP_PASSPORT,
                                                                HttpHeaders.TENANT_ID, HttpHeaders.X_VCAP_REQUEST_ID));
    }

}
