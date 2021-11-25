package com.sap.hcp.cf.log4j2.layout;

import static java.util.Arrays.asList;

import java.util.List;

import com.sap.hcp.cf.log4j2.converter.ContextPropsConverter;
import com.sap.hcp.cf.log4j2.converter.CustomFieldsConverter;
import com.sap.hcp.cf.log4j2.converter.Log4JStacktraceConverter;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;

/**
 * Defines the "built-in" layout pattern for our JSON encoder.
 *
 */
public final class LayoutPatternBuilder {

	/*
	 * -- all layout patterns end like this
	 */
	private static final String COMMON_POSTFIX_PATTERN = "}%n";

	private final StringBuilder sb;
    private final boolean sendDefaultValues;

	/*
	 * -- this defines the common prefix to all variants. -- the final line will
	 * add non-predefined context parameters from the MDC -- as this list may be
	 * empty, we use "replace" to at a colon if it's not
	 */
    public LayoutPatternBuilder(boolean sendDefaultValues) {
        this.sendDefaultValues = sendDefaultValues;
        this.sb = new StringBuilder("{ ");
		appendQuoted(Fields.WRITTEN_AT, "%d{yyyy-MM-dd'T'HH:mm:ss.SSSX}{UTC}").append(",");
		appendUnquoted(Fields.WRITTEN_TS, "%tstamp").append(",");
	}

	public LayoutPatternBuilder addRequestMetrics() {
		appendQuoted(Fields.TYPE, Defaults.TYPE_REQUEST).append(",");
		sb.append("%jsonmsg{flatten}").append(",");
		return this;
	}

	public LayoutPatternBuilder addBasicApplicationLogs() {
		appendQuoted(Fields.TYPE, Defaults.TYPE_LOG).append(",");
		appendQuoted(Fields.LOGGER, "%replace{%logger}{\"}{\\\\\"}").append(",");
		appendQuoted(Fields.THREAD, "%replace{%thread}{\"}{\\\\\"}").append(",");
		appendQuoted(Fields.LEVEL, "%p").append(",");
		appendUnquoted(Fields.CATEGORIES, "%categories").append(",");
		appendUnquoted(Fields.MSG, "%jsonmsg{escape}").append(",");
		return this;
	}

	public LayoutPatternBuilder addContextProperties(List<String> exclusions) {
		sb.append("%").append(ContextPropsConverter.WORD);
        appendParameters(asList(Boolean.toString(sendDefaultValues)));
		appendParameters(exclusions);
		return this;
	}

	public LayoutPatternBuilder addCustomFields(List<String> mdcKeyNames) {
		if (mdcKeyNames == null || mdcKeyNames.isEmpty()) {
			return this;
		}
		sb.append("\"").append(Fields.CUSTOM_FIELDS).append("\":");
		sb.append("{%").append(CustomFieldsConverter.WORD);
		appendParameters(mdcKeyNames);
		sb.append("}");
		sb.append(",");
		return this;
	}

	public LayoutPatternBuilder addStacktraces() {
		sb.append("\"").append(Fields.STACKTRACE).append("\":");
		sb.append("%").append(Log4JStacktraceConverter.WORD);
		sb.append(",");
		return this;
	}

	public LayoutPatternBuilder suppressExceptions() {
		removeTrailingComma();
		sb.append("%ex{0} ");
		sb.append(",");
		return this;
	}

	private StringBuilder appendQuoted(String key, String value) {
		return sb.append("\"").append(key).append("\":") //
				.append("\"").append(value).append("\"");
	}

	private StringBuilder appendUnquoted(String key, String value) {
		return sb.append("\"").append(key).append("\":").append(value);
	}

	private void appendParameters(List<String> parameters) {
		sb.append("{");
		for (int i = 0; i < parameters.size(); i++) {
			sb.append(parameters.get(i));
			if (i < parameters.size() - 1) {
				sb.append("}{");
			}
		}
		sb.append("}");
	}

	private void removeTrailingComma() {
		if (sb.length() == sb.lastIndexOf(",") + 1) {
			sb.delete(sb.length() - 1, sb.length());
		}
	}

	public String build() {
		removeTrailingComma();
		return sb.append(COMMON_POSTFIX_PATTERN).toString();
	}

}
