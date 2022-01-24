package com.sap.hcp.cf.log4j2.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.message.Message;

import com.sap.hcp.cf.logging.common.converter.DefaultPropertiesConverter;
import com.sap.hcp.cf.logging.common.customfields.CustomField;

/**
 * A simple {@link LogEventPatternConverter} implementation that converts
 * key/value pairs from the {@link org.slf4j.MDC}. These key/value pairs are
 * embedded in the JSON message, i.e. the appear as fields at the top-level JSON
 * object.
 * <p>
 * There are two exceptions to this:
 * <ol>
 * <li>The predefined keys from
 * {@link com.sap.hcp.cf.logging.common.Fields}</li>
 * <li>The list of key names that have been passed in as options.</li>
 * </ol>
 *
 */
@Plugin(name = "ContextPropsConverter", category = "Converter")
@ConverterKeys({ "ctxp" })
public class ContextPropsConverter extends LogEventPatternConverter {

    public static final String WORD = "ctxp";
    private final DefaultPropertiesConverter converter = new DefaultPropertiesConverter();

    public ContextPropsConverter(String[] options) {
        super(WORD, WORD);
        if (options != null) {
            converter.setSendDefaultValues(Boolean.parseBoolean(options[0]));
            converter.setExclusions(Arrays.asList(Arrays.copyOfRange(options, 1, options.length)));
        }
    }

    public static ContextPropsConverter newInstance(final String[] options) {
        return new ContextPropsConverter(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        Map<String, String> contextData = event.getContextData().toMap();
        contextData = addCustomFieldsFromArguments(contextData, event);
        int lengthBefore = toAppendTo.length();
        converter.convert(toAppendTo, contextData);
        // remove comma from pattern, when no properties are added
        // this is to avoid a double comma in the JSON
        // Do not do this on empty messages
        if (toAppendTo.length() == lengthBefore && lengthBefore > 0 && toAppendTo.charAt(lengthBefore - 1) == ',') {
            toAppendTo.setLength(lengthBefore - 1);
        }
    }

    private Map<String, String> addCustomFieldsFromArguments(Map<String, String> contextData, LogEvent event) {
        Message message = event.getMessage();
        Object[] parameters = message.getParameters();
        if (parameters == null) {
            return contextData;
        }
        boolean unchangedContextData = true;
        Map<String, String> result = contextData;
        for (Object current: parameters) {
            if (current instanceof CustomField) {
                CustomField field = (CustomField) current;
                if (unchangedContextData) {
                    // contextData might be an unmodifiable map
                    result = new HashMap<>(contextData);
                    unchangedContextData = false;
                }
                result.put(field.getKey(), field.getValue());
            }
        }
        return result;
    }

}
