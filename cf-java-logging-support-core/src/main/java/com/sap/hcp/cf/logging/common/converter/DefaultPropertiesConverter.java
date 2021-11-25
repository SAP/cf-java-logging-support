package com.sap.hcp.cf.logging.common.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.LogContext;

public class DefaultPropertiesConverter {

    private final Set<String> exclusions = new HashSet<String>();
    private boolean sendDefaultValues = false;

    public DefaultPropertiesConverter() {
    }

    public void setExclusions(List<String> exclusionList) {
        if (exclusionList != null) {
            for (String exclusion: exclusionList) {
                exclusions.add(exclusion);
            }
        }
    }

    public void setSendDefaultValues(boolean sendDefaultValues) {
        this.sendDefaultValues = sendDefaultValues;
    }

    private static class LoggerHolder {
        static final Logger LOG = LoggerFactory.getLogger(LoggerHolder.class.getEnclosingClass());
    }

    public void convert(StringBuilder appendTo, Map<String, String> eventProperties) {
        Map<String, String> properties = mergeContextMaps(eventProperties);
        if (properties != null && !properties.isEmpty()) {
            try {
                /*
                 * -- let's compose an JSON object, then chop off the outermost
                 * curly braces
                 */
                ObjectComposer<JSONComposer<String>> oc = JSON.std.composeString().startObject();
                for (Entry<String, String> p: properties.entrySet()) {
                    if (!exclusions.contains(p.getKey())) {
                        if (sendDefaultValues) {
                            oc.put(p.getKey(), p.getValue());
                        } else {
                            String defaultValue = getDefaultValue(p.getKey());
                            if (!defaultValue.equals(p.getValue())) {
                                oc.put(p.getKey(), p.getValue());
                            }
                        }
                    }
                }
                String result = oc.end().finish().trim();
                appendTo.append(result.substring(1, result.length() - 1));
            } catch (Exception ex) {
                /* -- avoids substitute logger warnings on startup -- */
                LoggerHolder.LOG.error("Conversion failed ", ex);
            }
        }
    }

    private Map<String, String> mergeContextMaps(Map<String, String> eventMap) {
        Map<String, String> result = new HashMap<String, String>();
        if (eventMap != null) {
            result.putAll(eventMap);
        }
        LogContext.loadContextFields();
        if (MDC.getCopyOfContextMap() != null) {
            for (Entry<String, String> e: MDC.getCopyOfContextMap().entrySet()) {
                if (!result.containsKey(e.getKey())) {
                    result.put(e.getKey(), e.getValue());
                }
            }
        }
        return result;
    }

    private String getDefaultValue(String key) {
        String defaultValue = LogContext.getDefault(key);
        return defaultValue == null ? Defaults.UNKNOWN : defaultValue;
    }
}
