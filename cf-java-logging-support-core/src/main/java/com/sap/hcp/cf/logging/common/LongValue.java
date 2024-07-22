package com.sap.hcp.cf.logging.common;

/**
 * A {@link Value} implementation representing a <i>long</i> value.
 */
public class LongValue implements Value {

    private long value;

    public LongValue(final long value) {
        this.value = value;
    }

    public LongValue(Object value) {
        if (value != null && Long.class.isAssignableFrom(value.getClass())) {
            this.value = ((Long) value).longValue();
        } else {
            this.value = -1L;
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public Object getValue() {
        return Long.valueOf(value);
    }

    @Override
    public long asLong() {
        return value;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public String asString() {
        return toString();
    }
}
