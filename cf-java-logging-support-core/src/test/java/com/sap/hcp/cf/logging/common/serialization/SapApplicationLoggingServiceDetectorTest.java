package com.sap.hcp.cf.logging.common.serialization;

import com.sap.hcp.cf.logging.common.helper.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sap.hcp.cf.logging.common.serialization.SapApplicationLoggingTestBindings.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SapApplicationLoggingServiceDetectorTest {

    @Mock
    private Environment environment;

    @Test
    void nonCloudFoundryEnvironment() {
        SapApplicationLoggingServiceDetector detector = new SapApplicationLoggingServiceDetector(environment);
        assertFalse(detector.isBoundToSapApplicationLogging(), "not bound to SAP Application Logging service");
    }

    @Test
    void emptyBindings() {
        when(environment.getVariable(Environment.VCAP_SERRVICES)).thenReturn("{}");
        SapApplicationLoggingServiceDetector detector = new SapApplicationLoggingServiceDetector(environment);
        assertFalse(detector.isBoundToSapApplicationLogging(), "not bound to SAP Application Logging service");
    }

    @Test
    void noApplicationLoggingBinding() {
        SapApplicationLoggingServiceDetector detector = NO_SAP_APPLICATION_LOGGING_BINDING.getDetector();
        assertFalse(detector.isBoundToSapApplicationLogging(), "not bound to SAP Application Logging service");
    }

    @Test
    void noApplicationLoggingBindingButOverridenByEnvironment() {
        SapApplicationLoggingServiceDetector detector = NO_SAP_APPLICATION_LOGGING_BINDING_BUT_OVERRIDE.getDetector();
        assertTrue(detector.isBoundToSapApplicationLogging(),
                   "not bound to SAP Application Logging service and no override");
    }

    @Test
    void justOneApplicationLoggingBinding() {
        SapApplicationLoggingServiceDetector detector = JUST_ONE_SAP_APPLICATION_LOGGING_BINDING.getDetector();
        assertTrue(detector.isBoundToSapApplicationLogging(), "bound to SAP Application Logging service");
    }

    @Test
    void mixedNoOneApplicationLoggingBinding() {
        SapApplicationLoggingServiceDetector detector = MIXED_SAP_APPLICATION_LOGGING_AND_OTHER_BINDING.getDetector();
        assertTrue(detector.isBoundToSapApplicationLogging(), "bound to SAP Application Logging service");
    }
}
