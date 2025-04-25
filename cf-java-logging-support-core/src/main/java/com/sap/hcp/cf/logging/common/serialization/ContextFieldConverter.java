package com.sap.hcp.cf.logging.common.serialization;

import com.fasterxml.jackson.jr.ob.comp.ArrayComposer;
import com.fasterxml.jackson.jr.ob.comp.ComposerBase;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContextFieldConverter {

    private final boolean sendDefaultValues;
    private final List<String> customFieldMdcKeyNames;
    private final List<String> retainFieldMdcKeyNames;

    public ContextFieldConverter(boolean sendDefaultValues, List<String> customFieldMdcKeyNames,
                                 List<String> retainFieldMdcKeyNames) {
        this(sendDefaultValues, customFieldMdcKeyNames, retainFieldMdcKeyNames,
             new SapApplicationLoggingServiceDetector());
    }

    ContextFieldConverter(boolean sendDefaultValues, List<String> customFieldMdcKeyNames,
                          List<String> retainFieldMdcKeyNames,
                          SapApplicationLoggingServiceDetector sapApplicationLoggingServiceDetector) {
        this.sendDefaultValues = sendDefaultValues;
        this.retainFieldMdcKeyNames = retainFieldMdcKeyNames;
        if (sapApplicationLoggingServiceDetector.isBoundToSapApplicationLogging()) {
            // only create #cf structure, if SAP Application Logging Service is bound
            this.customFieldMdcKeyNames = customFieldMdcKeyNames;
        } else {
            this.customFieldMdcKeyNames = Collections.emptyList();
        }

    }

    public <P extends ComposerBase> void addContextFields(ObjectComposer<P> oc, Map<String, Object> contextFields) {
        contextFields.keySet().stream().filter(this::isContextField)
                     .forEach(n -> addContextField(oc, n, contextFields.get(n)));
    }

    private boolean isContextField(String name) {
        return retainFieldMdcKeyNames.contains(name) || !customFieldMdcKeyNames.contains(name);
    }

    private <P extends ComposerBase> void addContextField(ObjectComposer<P> oc, String name, Object value) {
        try {
            if (sendDefaultValues) {
                put(oc, name, value);
            } else {
                String defaultValue = getDefaultValue(name);
                if (!defaultValue.equals(value)) {
                    put(oc, name, value);
                }
            }
        } catch (IOException ignored) {
            try {
                oc.put(name, "invalid value");
            } catch (IOException cause) {
                throw new JsonSerializationException("Cannot create field \"" + name + "\".", cause);
            }
        }
    }

    private <P extends ComposerBase> void put(ObjectComposer<P> oc, String name, Object value) throws IOException {
        if (value instanceof String) {
            oc.put(name, (String) value);
        } else if (value instanceof Long) {
            oc.put(name, ((Long) value).longValue());
        } else if (value instanceof Double) {
            oc.put(name, ((Double) value).doubleValue());
        } else if (value instanceof Boolean) {
            oc.put(name, ((Boolean) value).booleanValue());
        } else if (value instanceof Integer) {
            oc.put(name, ((Integer) value).intValue());
        } else if (value instanceof Float) {
            oc.put(name, ((Float) value).floatValue());
        } else {
            oc.put(name, String.valueOf(value));
        }
    }

    private String getDefaultValue(String key) {
        String defaultValue = LogContext.getDefault(key);
        return defaultValue == null ? Defaults.UNKNOWN : defaultValue;
    }

    public <P extends ComposerBase> void addCustomFields(ObjectComposer<P> oc, Map<String, Object> contextFields)
            throws IOException {
        ArrayComposer<ObjectComposer<ObjectComposer<P>>> customFieldComposer = null;
        for (int i = 0; i < customFieldMdcKeyNames.size(); i++) {
            String key = customFieldMdcKeyNames.get(i);
            Object value = contextFields.get(key);
            if (value != null) {
                if (customFieldComposer == null) {
                    customFieldComposer = oc.startObjectField(Fields.CUSTOM_FIELDS).startArrayField("string");
                }
                customFieldComposer.startObject().put("k", key).put("v", String.valueOf(value)).put("i", i).end();
            }
        }
        if (customFieldComposer != null) {
            customFieldComposer.end().end();
        }
    }
}
