package com.sap.hcp.cf.logging.common;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Predefined {@link Marker} instances that are used in the layout pattern configuration.
 */
public class Markers {

    public static final Marker DEFAULT_MARKER = MarkerFactory.getMarker("_default_");
    public static final Marker EXCEPTION_MARKER = MarkerFactory.getMarker("exception");
    public static final Marker REQUEST_MARKER = MarkerFactory.getMarker("request");

}
