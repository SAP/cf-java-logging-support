package com.sap.hcp.cf.logback.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.converter.DefaultPropertiesConverter;
import com.sap.hcp.cf.logging.common.customfields.CustomField;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A simple {@link ClassicConverter} implementation that converts key/value
 * pairs from the {@link org.slf4j.MDC}. These key/value pairs are embedded in
 * the JSON message, i.e. the appear as fields at the top-level JSON object.
 * <p>
 * There are two exceptions to this:
 * <ol>
 * <li>The predefined keys from
 * {@link com.sap.hcp.cf.logging.common.Fields}</li>
 * <li>The list of key names that have been passed in as options.</li>
 * </ol>
 *
 */
public class ContextPropsConverter extends ClassicConverter {

	public static final String WORD = "ctxp";
	private DefaultPropertiesConverter converter = new DefaultPropertiesConverter();
	private CustomFieldsAdapter customFieldsAdapter = new CustomFieldsAdapter();

	void setConverter(DefaultPropertiesConverter converter) {
		this.converter = converter;
	}

	void setCustomFieldsAdapter(CustomFieldsAdapter customFieldsAdapter) {
		this.customFieldsAdapter = customFieldsAdapter;
	}

	@Override
	public String convert(ILoggingEvent event) {
		StringBuilder appendTo = new StringBuilder();
		LogContext.loadContextFields();
		Map<String, String> propertyMap = new HashMap<>(event.getMDCPropertyMap());
		addCustomFieldsFromArgument(propertyMap, event);
		converter.convert(appendTo, propertyMap);
		return appendTo.toString();
	}

	private void addCustomFieldsFromArgument(Map<String, String> propertyMap, ILoggingEvent event) {
		Object[] arguments = event.getArgumentArray();
		if (arguments == null) {
			return;
		}
		for (Object current : arguments) {
			if (current instanceof CustomField) {
				CustomField field = (CustomField) current;
				propertyMap.put(field.getKey(), field.getValue());
			}

		}
	}

	@Override
	public void start() {
		customFieldsAdapter.initialize(getContext());
		converter.setExclusions(calculateExclusions());
        converter.setSendDefaultValues(customFieldsAdapter.isSendDefaultValues());
		super.start();
	}

	private List<String> calculateExclusions() {
		List<String> exclusions = new ArrayList<>(customFieldsAdapter.getCustomFieldExclusions());
		if (getOptionList() != null) {
			exclusions.addAll(getOptionList());
		}
		return exclusions;
	}
}
