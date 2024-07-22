package com.sap.hcp.cf.logging.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A {@link Value} implementation representing a <i>double</i> value.
 */
public class DoubleValue implements Value {

    private double value;

    public DoubleValue(Object value) {
        if (value != null && Double.class.isAssignableFrom(value.getClass())) {
            this.value = ((Double) value).doubleValue();
        } else {
            this.value = 0.0;
        }
    }

    public DoubleValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(value);
    }

    @Override
    public Object getValue() {
        return Double.valueOf(value);
    }

    @Override
    public long asLong() {
        return (long) value;
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
