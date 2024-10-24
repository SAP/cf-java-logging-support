package com.sap.hcp.cf.logging.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.hcp.cf.logging.common.helper.Environment;

import org.junit.jupiter.api.Test;

public class LogOptionalFieldsSettingsTest {

    @Test
    public void testLogOptionalFieldsSettingsTrue() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("true");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("True");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("TRUE");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("true");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertTrue(settings.isLogSensitiveConnectionData(), "Wrapping LOG_SENSITIVE_CONNECTION_DATA failed");
        assertTrue(settings.isLogRemoteUserField(), "Wrapping LOG_REMOTE_USER failed");
        assertTrue(settings.isLogRefererField(), "Wrapping LOG_REFERER failed");
        assertTrue(settings.isLogSslHeaders(), "Wrapping LOG_SSL_HEADERS failed");
    }

    @Test
    public void testLogOptionalFieldsSettingsFalse() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("false");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("False");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("FALSE");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("false");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertFalse(settings.isLogSensitiveConnectionData(), "Wrapping LOG_SENSITIVE_CONNECTION_DATA failed");
        assertFalse(settings.isLogRemoteUserField(), "Wrapping LOG_REMOTE_USER failed");
        assertFalse(settings.isLogRefererField(), "Wrapping LOG_REFERER failed");
        assertFalse(settings.isLogSslHeaders(), "Wrapping LOG_SSL_HEADERS failed");
    }

    @Test
    public void testLogOptionalFieldsSettingsInvalidEnvVariable() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("someInvalidString");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("someInvalidString");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("someInvalidString");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("someInvalidString");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");
        assertFalse(settings.isLogSensitiveConnectionData(), "Wrapping LOG_SENSITIVE_CONNECTION_DATA failed");
        assertFalse(settings.isLogRemoteUserField(), "Wrapping LOG_REMOTE_USER failed");
        assertFalse(settings.isLogRefererField(), "Wrapping LOG_REFERER failed");
        assertFalse(settings.isLogSslHeaders(), "Wrapping LOG_SSL_HEADERS failed");
    }

    @Test
    public void testLogOptionalFieldsSettingsEmptyString() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");
        assertFalse(settings.isLogSensitiveConnectionData(), "Wrapping LOG_SENSITIVE_CONNECTION_DATA failed");
        assertFalse(settings.isLogRemoteUserField(), "Wrapping LOG_REMOTE_USER failed");
        assertFalse(settings.isLogRefererField(), "Wrapping LOG_REFERER failed");
        assertFalse(settings.isLogSslHeaders(), "Wrapping LOG_SSL_HEADERS failed");
    }

    @Test
    public void testLogOptionalFieldsSettingsEmptyEnvVariable() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn(null);
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn(null);
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn(null);
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn(null);

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertFalse(settings.isLogSensitiveConnectionData(), "Wrapping LOG_SENSITIVE_CONNECTION_DATA failed");
        assertFalse(settings.isLogRemoteUserField(), "Wrapping LOG_REMOTE_USER failed");
        assertFalse(settings.isLogRefererField(), "Wrapping LOG_REFERER failed");
        assertFalse(settings.isLogSslHeaders(), "Wrapping LOG_SSL_HEADERS failed");
    }

    @Test
    public void testLogOptionalFieldsWithMixedSettings() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("false");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("true");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("True");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("False");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertFalse(settings.isLogSensitiveConnectionData(), "Wrapping LOG_SENSITIVE_CONNECTION_DATA failed");
        assertTrue(settings.isLogRemoteUserField(), "Wrapping LOG_REMOTE_USER failed");
        assertTrue(settings.isLogRefererField(), "Wrapping LOG_REFERER failed");
        assertFalse(settings.isLogSslHeaders(), "Wrapping LOG_SSL_HEADERS failed");
    }

}
