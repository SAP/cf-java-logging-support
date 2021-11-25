package com.sap.hcp.cf.log4j2.layout;

import static java.util.Collections.emptyList;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.MarkerPatternSelector;
import org.apache.logging.log4j.core.layout.PatternMatch;
import org.apache.logging.log4j.core.layout.PatternSelector;
import org.apache.logging.log4j.core.pattern.PatternFormatter;

import com.sap.hcp.cf.logging.common.Markers;

@Plugin(name = "JsonPatternLayout", category = "Core", elementType = "Layout", printObject = true)
public final class JsonPatternLayout extends AbstractStringLayout {

    private final PatternSelector markerSelector;
    private final PatternSelector execptionSelector;

    private final Configuration config;
    private final CustomFieldsAdapter customFieldsAdapter;
    private final boolean sendDefaultValues;

    protected JsonPatternLayout(Configuration config, Charset charset, boolean sendDefaultValues,
                                CustomField... customFieldMdcKeyNames) {
        super(charset);
        this.config = config;
        this.sendDefaultValues = sendDefaultValues;
        this.customFieldsAdapter = new CustomFieldsAdapter(customFieldMdcKeyNames);
        markerSelector = createPatternSelector(customFieldMdcKeyNames);
        execptionSelector = createExceptionSelector(customFieldMdcKeyNames);
    }

    @Override
    public String toSerializable(LogEvent event) {
        PatternSelector selector = getSelector(event);
        final StringBuilder buf = getStringBuilder();
        PatternFormatter[] formatters = selector.getFormatters(event);
        final int len = formatters.length;
        for (int i = 0; i < len; i++) {
            formatters[i].format(event, buf);
        }
        String str = buf.toString();
        return str;
    }

    @PluginFactory
    public static JsonPatternLayout createLayout(@PluginAttribute(value = "charset") final Charset charset,
                                                 @PluginAttribute(value = "sendDefaultValues") final boolean sendDefaultValues,
                                                 @PluginElement(value = "customField") CustomField[] customFieldMdcKeyNames,
                                                 @PluginConfiguration final Configuration config) {
        return new JsonPatternLayout(config, charset, sendDefaultValues, customFieldMdcKeyNames);
    }

    private MarkerPatternSelector createPatternSelector(CustomField... customFieldMdcKeyNames) {
        PatternMatch[] pMatches = new PatternMatch[1];
        String requestPattern = new LayoutPatternBuilder(sendDefaultValues).addContextProperties(emptyList())
                                                                           .addRequestMetrics().suppressExceptions()
                                                                           .build();
        pMatches[0] = new PatternMatch(Markers.REQUEST_MARKER.getName(), requestPattern);
        List<String> customFields = customFieldsAdapter.getCustomFieldKeyNames();
        List<String> excludedFields = customFieldsAdapter.getExcludedFieldKeyNames();
        String applicationPattern = new LayoutPatternBuilder(sendDefaultValues).addBasicApplicationLogs()
                                                                               .addContextProperties(excludedFields)
                                                                               .addCustomFields(customFields)
                                                                               .suppressExceptions().build();
        return new MarkerPatternSelector.Builder().setProperties(pMatches).setDefaultPattern(applicationPattern)
                                                  .setAlwaysWriteExceptions(false).setNoConsoleNoAnsi(false)
                                                  .setConfiguration(config).build();
    }

    private PatternSelector createExceptionSelector(CustomField... customFieldMdcKeyNames) {
        List<String> customFields = customFieldsAdapter.getCustomFieldKeyNames();
        List<String> excludedFields = customFieldsAdapter.getExcludedFieldKeyNames();
        String exceptionPattern = new LayoutPatternBuilder(sendDefaultValues).addBasicApplicationLogs()
                                                                             .addContextProperties(excludedFields)
                                                                             .addCustomFields(customFields)
                                                                             .addStacktraces().build();
        return new MarkerPatternSelector(new PatternMatch[0], exceptionPattern, false, false, config);
    }

    private PatternSelector getSelector(LogEvent event) {
        if (event.getThrownProxy() != null || event.getThrown() != null) {
            return execptionSelector;
        }
        return markerSelector;
    }
}
