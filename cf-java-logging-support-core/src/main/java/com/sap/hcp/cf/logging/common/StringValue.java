package com.sap.hcp.cf.logging.common;

/**
 * A {@link Value} implementation representing a <i>string</i>.
 */
public class StringValue implements Value {

    public static final String UNDEFINED = "-";

    private final String value;

    public StringValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public Object getValue() {
        return String.valueOf(value);
    }

    @Override
    public long asLong() {
        return Long.parseLong(value);
    }

    @Override
    public double asDouble() {
        return Double.parseDouble(value);
    }

    @Override
    public String asString() {
        return value;
    }
}
