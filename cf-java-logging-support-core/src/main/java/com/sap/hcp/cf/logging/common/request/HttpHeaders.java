package com.sap.hcp.cf.logging.common.request;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;

public enum HttpHeaders implements HttpHeader {
                                               CONTENT_LENGTH("content-length"), //
                                               CONTENT_TYPE("content-type"), //
                                               REFERER("referer"), //
                                               X_CUSTOM_HOST("x-custom-host", Fields.X_CUSTOM_HOST), //
                                               X_FORWARDED_FOR("x-forwarded-for", Fields.X_FORWARDED_FOR), //
                                               X_FORWARDED_HOST("x-forwarded-host", Fields.X_FORWARDED_HOST), //
                                               X_FORWARDED_PROTO("x-forwarded-proto", Fields.X_FORWARDED_PROTO), //
                                               X_SSL_CLIENT("x-ssl-client", Fields.X_SSL_CLIENT), //
                                               X_SSL_CLIENT_VERIFY("x-ssl-client-verify", Fields.X_SSL_CLIENT_VERIFY), //
                                               X_SSL_CLIENT_SUBJECT_DN("x-ssl-client-subject-dn",
                                                                       Fields.X_SSL_CLIENT_SUBJECT_DN), //
                                               X_SSL_CLIENT_SUBJECT_CN("x-ssl-client-subject-cn",
                                                                       Fields.X_SSL_CLIENT_SUBJECT_CN), //
                                               X_SSL_CLIENT_ISSUER_DN("x-ssl-client-issuer-dn",
                                                                      Fields.X_SSL_CLIENT_ISSUER_DN), //
                                               X_SSL_CLIENT_NOTBEFORE("x-ssl-client-notbefore",
                                                                      Fields.X_SSL_CLIENT_NOTBEFORE), //
                                               X_SSL_CLIENT_NOTAFTER("x-ssl-client-notafter",
                                                                     Fields.X_SSL_CLIENT_NOTAFTER), //
                                               X_SSL_CLIENT_SESSION_ID("x-ssl-client-session-id",
                                                                       Fields.X_SSL_CLIENT_SESSION_ID), //
                                               X_VCAP_REQUEST_ID("x-vcap-request-id", Fields.REQUEST_ID, true), //
                                               CORRELATION_ID("X-CorrelationID", Fields.CORRELATION_ID, true,
                                                              X_VCAP_REQUEST_ID), //
                                               SAP_PASSPORT("sap-passport", Fields.SAP_PASSPORT, true), //
                                               TENANT_ID("tenantid", Fields.TENANT_ID, true); //

    private HttpHeaders(String name) {
        this(name, null);
    }

    private HttpHeaders(String name, String field) {
        this(name, field, false);
    }

    private HttpHeaders(String name, String field, boolean isPropagated, HttpHeaders... aliases) {
        this.name = name;
        this.field = field;
        this.isPropagated = isPropagated;
        this.aliases = unmodifiableList(asList(aliases));
    }

    private String name;
    private String field;
    private boolean isPropagated;
    private List<HttpHeader> aliases;

    @Override
    public boolean isPropagated() {
        return isPropagated;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public String getFieldValue() {
        return field != null ? LogContext.get(field) : Defaults.UNKNOWN;
    }

    @Override
    public List<HttpHeader> getAliases() {
        return aliases;
    }

    public static List<HttpHeaders> propagated() {
        return LazyPropagatedHeaderHolder.PROPAGATED;
    }

    private static class LazyPropagatedHeaderHolder {
        public static final List<HttpHeaders> PROPAGATED = createPropagated();

        private static List<HttpHeaders> createPropagated() {
            List<HttpHeaders> propagated = new ArrayList<>();
            for (HttpHeaders current: values()) {
                if (current.isPropagated()) {
                    propagated.add(current);
                }
            }
            return Collections.unmodifiableList(propagated);
        }
    }
}
