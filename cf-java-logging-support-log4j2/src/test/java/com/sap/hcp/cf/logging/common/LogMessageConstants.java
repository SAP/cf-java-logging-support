package com.sap.hcp.cf.logging.common;

public interface LogMessageConstants {
    String TEST_MESSAGE = "this is a test message";
    // see log4j2-test.xml for valid field keys and indices
    String CUSTOM_FIELD_KEY = "custom-field";
    int CUSTOM_FIELD_INDEX = 0;
    String TEST_FIELD_KEY = "test-field";
    int TEST_FIELD_INDEX = 1;
    String RETAINED_FIELD_KEY = "retained-field";
    int RETAINED_FIELD_INDEX = 2;
    String SOME_KEY = "some_key";
    String SOME_VALUE = "some value";
    String SOME_OTHER_VALUE = "some other value";
    String HACK_ATTEMPT = "}{:\",\"";
}
