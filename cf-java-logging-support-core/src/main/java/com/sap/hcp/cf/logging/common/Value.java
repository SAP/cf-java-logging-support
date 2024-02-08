package com.sap.hcp.cf.logging.common;

/**
 * A simple <i>value</i> interface to store, well, a value. We're not into generics here, but rather use a simple
 * interface that allows us to store different value types in a map.
 */
public interface Value {

    public Object getValue();

    public long asLong();

    public double asDouble();

    public String asString();
}
