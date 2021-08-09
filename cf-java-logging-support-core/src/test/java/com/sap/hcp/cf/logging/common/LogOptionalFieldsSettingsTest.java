package com.sap.hcp.cf.logging.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.sap.hcp.cf.logging.common.helper.Environment;

public class LogOptionalFieldsSettingsTest {

    @Test
    public void testLogOptionalFieldsSettingsTrue() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("true");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("True");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("TRUE");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("true");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertTrue("Wrapping LOG_SENSITIVE_CONNECTION_DATA failed", settings.isLogSensitiveConnectionData());
        assertTrue("Wrapping LOG_REMOTE_USER failed", settings.isLogRemoteUserField());
        assertTrue("Wrapping LOG_REFERER failed", settings.isLogRefererField());
        assertTrue("Wrapping LOG_SSL_HEADERS failed", settings.isLogSslHeaders());
    }

    @Test
    public void testLogOptionalFieldsSettingsFalse() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("false");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("False");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("FALSE");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("false");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertFalse("Wrapping LOG_SENSITIVE_CONNECTION_DATA failed", settings.isLogSensitiveConnectionData());
        assertFalse("Wrapping LOG_REMOTE_USER failed", settings.isLogRemoteUserField());
        assertFalse("Wrapping LOG_REFERER failed", settings.isLogRefererField());
        assertFalse("Wrapping LOG_SSL_HEADERS failed", settings.isLogSslHeaders());
    }

    @Test
    public void testLogOptionalFieldsSettingsInvalidEnvVariable() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("someInvalidString");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("someInvalidString");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("someInvalidString");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("someInvalidString");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");
        assertFalse("Wrapping LOG_SENSITIVE_CONNECTION_DATA failed", settings.isLogSensitiveConnectionData());
        assertFalse("Wrapping LOG_REMOTE_USER failed", settings.isLogRemoteUserField());
        assertFalse("Wrapping LOG_REFERER failed", settings.isLogRefererField());
        assertFalse("Wrapping LOG_SSL_HEADERS failed", settings.isLogSslHeaders());
    }

    @Test
    public void testLogOptionalFieldsSettingsEmptyString() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");
        assertFalse("Wrapping LOG_SENSITIVE_CONNECTION_DATA failed", settings.isLogSensitiveConnectionData());
        assertFalse("Wrapping LOG_REMOTE_USER failed", settings.isLogRemoteUserField());
        assertFalse("Wrapping LOG_REFERER failed", settings.isLogRefererField());
        assertFalse("Wrapping LOG_SSL_HEADERS failed", settings.isLogSslHeaders());
    }

    @Test
    public void testLogOptionalFieldsSettingsEmptyEnvVariable() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn(null);
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn(null);
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn(null);
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn(null);

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertFalse("Wrapping LOG_SENSITIVE_CONNECTION_DATA failed", settings.isLogSensitiveConnectionData());
        assertFalse("Wrapping LOG_REMOTE_USER failed", settings.isLogRemoteUserField());
        assertFalse("Wrapping LOG_REFERER failed", settings.isLogRefererField());
        assertFalse("Wrapping LOG_SSL_HEADERS failed", settings.isLogSslHeaders());
    }

    @Test
    public void testLogOptionalFieldsWithMixedSettings() {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getVariable(Environment.LOG_SENSITIVE_CONNECTION_DATA)).thenReturn("false");
        when(mockEnvironment.getVariable(Environment.LOG_REMOTE_USER)).thenReturn("true");
        when(mockEnvironment.getVariable(Environment.LOG_REFERER)).thenReturn("True");
        when(mockEnvironment.getVariable(Environment.LOG_SSL_HEADERS)).thenReturn("False");

        LogOptionalFieldsSettings settings = new LogOptionalFieldsSettings(mockEnvironment, "NameOfInvokingClass");

        assertFalse("Wrapping LOG_SENSITIVE_CONNECTION_DATA failed", settings.isLogSensitiveConnectionData());
        assertTrue("Wrapping LOG_REMOTE_USER failed", settings.isLogRemoteUserField());
        assertTrue("Wrapping LOG_REFERER failed", settings.isLogRefererField());
        assertFalse("Wrapping LOG_SSL_HEADERS failed", settings.isLogSslHeaders());
    }

}
