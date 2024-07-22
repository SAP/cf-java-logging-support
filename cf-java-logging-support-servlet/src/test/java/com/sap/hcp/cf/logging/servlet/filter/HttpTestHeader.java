package com.sap.hcp.cf.logging.servlet.filter;

import com.sap.hcp.cf.logging.common.request.HttpHeader;

import java.util.Arrays;
import java.util.List;

public class HttpTestHeader implements HttpHeader {

    private final String name;
    private final String field;
    private final String fieldValue;
    private final boolean propagated;
    private final List<HttpHeader> aliases;

    public HttpTestHeader(String name, String field, String fieldValue, boolean propagated, HttpHeader... aliases) {
        this.name = name;
        this.field = field;
        this.fieldValue = fieldValue;
        this.propagated = propagated;
        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public boolean isPropagated() {
        return propagated;
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
    public List<HttpHeader> getAliases() {
        return aliases;
    }

    @Override
    public String getFieldValue() {
        return fieldValue;
    }

}
