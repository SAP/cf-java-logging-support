package com.sap.hcp.cf.logging.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;
import java.util.Map.Entry;

import static com.sap.hcp.cf.logging.common.Fields.CORRELATION_ID;

public class LogContext {

    @SuppressWarnings("serial")
    private static Map<String, String> CTX_FIELDS = new HashMap<String, String>() {
        {
            put(Fields.CORRELATION_ID, Defaults.UNKNOWN);
            put(Fields.TENANT_ID, Defaults.UNKNOWN);
            put(Fields.TENANT_SUBDOMAIN, Defaults.UNKNOWN);
            put(Fields.COMPONENT_ID, Defaults.UNKNOWN);
            put(Fields.COMPONENT_NAME, Defaults.UNKNOWN);
            put(Fields.COMPONENT_TYPE, Defaults.COMPONENT_TYPE);
            put(Fields.COMPONENT_INSTANCE, Defaults.COMPONENT_INDEX);
            put(Fields.CONTAINER_ID, Defaults.UNKNOWN);
            put(Fields.ORGANIZATION_ID, Defaults.UNKNOWN);
            put(Fields.ORGANIZATION_NAME, Defaults.UNKNOWN);
            put(Fields.SPACE_ID, Defaults.UNKNOWN);
            put(Fields.SPACE_NAME, Defaults.UNKNOWN);
        }
    };

    public static void loadContextFields(boolean override) {
        /*
         * -- do bootstrap, either enforced or because map is empty
         */
        if (override || MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty()) {
            MDC.setContextMap(VcapEnvReader.getEnvMap());
        } else {
            /* -- map is not empty, but we're missing important stuff -- */
            if (!MDC.getCopyOfContextMap().containsKey(Fields.COMPONENT_ID)) {
                for (Entry<String, String> envTag: VcapEnvReader.getEnvMap().entrySet()) {
                    if (override || MDC.get(envTag.getKey()) == null) {
                        MDC.put(envTag.getKey(), envTag.getValue());
                    }
                }
            }
        }
        for (Entry<String, String> ctxTag: CTX_FIELDS.entrySet()) {
            if (override || MDC.get(ctxTag.getKey()) == null && ctxTag.getValue() != null) {
                MDC.put(ctxTag.getKey(), ctxTag.getValue());
            }
        }
    }

    public static void loadContextFields() {
        loadContextFields(false);
    }

    public static void resetContextFields() {
        for (String ctxTag: CTX_FIELDS.keySet()) {
            MDC.remove(ctxTag);
        }
    }

    public static Collection<String> getContextFieldsKeys() {
        return Collections.unmodifiableSet(CTX_FIELDS.keySet());
    }

    public static String getDefault(String key) {
        return CTX_FIELDS.get(key);
    }

    public static void initializeContext(String correlationIdFromHeader) {
        loadContextFields(false);
        setOrGenerateCorrelationId(correlationIdFromHeader);
    }

    public static String get(String key) {
        return MDC.get(key);
    }

    public static String add(String key, String value) {
        MDC.put(key, value);
        return value;
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    public static void initializeContext() {
        initializeContext(null);
    }

    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }

    private static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }

    private static void setOrGenerateCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isEmpty() || correlationId.equals(Defaults.UNKNOWN)) {
            generateAndSetCorrelationId();
        } else {
            setCorrelationId(correlationId);
        }
    }

    private static void generateAndSetCorrelationId() {
        String generatedCorrelationId = String.valueOf(UUID.randomUUID());
        setCorrelationId(generatedCorrelationId);

        LoggerHolder.LOG.info("generated new correlation id");
    }

    private static class LoggerHolder {
        static final Logger LOG = LoggerFactory.getLogger(LoggerHolder.class.getEnclosingClass());
    }
}
