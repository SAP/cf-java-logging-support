package com.sap.hcp.cf.logback.encoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Marker;

import com.sap.hcp.cf.logback.converter.CategoriesConverter;
import com.sap.hcp.cf.logback.converter.ContextPropsConverter;
import com.sap.hcp.cf.logback.converter.CustomFieldsAdapter;
import com.sap.hcp.cf.logback.converter.CustomFieldsConverter;
import com.sap.hcp.cf.logback.converter.JsonMessageConverter;
import com.sap.hcp.cf.logback.converter.LogbackStacktraceConverter;
import com.sap.hcp.cf.logback.converter.TimestampConverter;
import com.sap.hcp.cf.logback.encoder.LayoutPatterns.PATTERN_KEY;
import com.sap.hcp.cf.logging.common.Markers;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.PostCompileProcessor;

/**
 * A {@link LayoutWrappingEncoder} implementation that encodes an
 * {@link ILoggingEvent} as a JSON object.
 * <p>
 * Under the hood, it's using a couple of {@link PatternLayout} instances to
 * support different logging contexts. These patterns are specified in
 * {@link LayoutPatterns}, so there's no need/way to do that in the logback
 * configuration file. There, you should only specify that you use this encoder
 * with an appender, for example like this: <blockquote>
 * 
 * <pre>
 * &lt;appender name="STDOUT-JSON" class="ch.qos.logback.core.ConsoleAppender"&gt;
 *    &lt;encoder class="com.sap.hcp.cf.logback.encoder.JsonEncoder"/&gt;
 * &lt;/appender&gt;
 * </pre>
 * 
 * </blockquote>
 */
public class JsonEncoder extends LayoutWrappingEncoder<ILoggingEvent> {


	public static class JsonLayout extends LayoutBase<ILoggingEvent> {

		private final Map<Marker, PatternLayout> layouts = new HashMap<Marker, PatternLayout>();

		@Override
		public void start() {
			defineConverters();
			initPatterns();
			super.start();
		}

		@Override
		public String doLayout(ILoggingEvent event) {
			return getLayout(event).doLayout(event);
		}

		private PatternLayout getLayout(ILoggingEvent event) {
			PatternLayout layout = layouts.get(getMarker(event));
			if (layout == null) {
				layout = layouts.get(Markers.DEFAULT_MARKER);
			}
			return layout;
		}

		private Marker getMarker(ILoggingEvent event) {
			if (hasException(event)) {
				return Markers.EXCEPTION_MARKER;
			}
			Marker m = event.getMarker();
			if (m == null) {
				m = Markers.DEFAULT_MARKER;
			}
			return m;
		}

		private boolean hasException(ILoggingEvent event) {
			return event.getThrowableProxy() != null;
		}

		private void defineConverters() {
			PatternLayout.defaultConverterMap.put(CustomFieldsConverter.WORD, CustomFieldsConverter.class.getName());
			PatternLayout.defaultConverterMap.put(JsonMessageConverter.WORD, JsonMessageConverter.class.getName());
			PatternLayout.defaultConverterMap.put(ContextPropsConverter.WORD, ContextPropsConverter.class.getName());
			PatternLayout.defaultConverterMap.put(LogbackStacktraceConverter.WORD,
					LogbackStacktraceConverter.class.getName());
			PatternLayout.defaultConverterMap.put(TimestampConverter.WORD, TimestampConverter.class.getName());
			PatternLayout.defaultConverterMap.put(CategoriesConverter.WORD, CategoriesConverter.class.getName());
		}

		private void initPatterns() {
			PostCompileProcessor<ILoggingEvent> postProcessor = new ChildConverterContextInjector();

			PatternLayout pl = new PatternLayout();
			pl.setPattern(LayoutPatterns.getPattern(PATTERN_KEY.APPLICATION));
			pl.setContext(context);
			pl.setPostCompileProcessor(postProcessor);
			pl.start();
			layouts.put(Markers.DEFAULT_MARKER, pl);

			pl = new PatternLayout();
			pl.setPattern(LayoutPatterns.getPattern(PATTERN_KEY.EXCEPTION));
			pl.setContext(context);
			pl.setPostCompileProcessor(postProcessor);
			pl.start();
			layouts.put(Markers.EXCEPTION_MARKER, pl);

			pl = new PatternLayout();
			pl.setPattern(LayoutPatterns.getPattern(PATTERN_KEY.REQUEST));
			pl.setContext(context);
			pl.setPostCompileProcessor(postProcessor);
			pl.start();
			layouts.put(Markers.REQUEST_MARKER, pl);
		}

	}

	private List<String> customFieldMdcKeyNames = new ArrayList<>();
	private List<String> retainFieldMdcKeyNames = new ArrayList<>();
    private boolean sendDefaultValues = false;

	public void addCustomFieldMdcKeyName(String name) {
        customFieldMdcKeyNames.add(name);
	}

	public void addRetainFieldMdcKeyName(String name) {
		retainFieldMdcKeyNames.add(name);
	}

    public void setSendDefaultValues(boolean sendDefaultValues) {
        this.sendDefaultValues = sendDefaultValues;
    }

	@Override
	public void start() {
		context.putObject(CustomFieldsAdapter.OPTION_MDC_CUSTOM_FIELDS, customFieldMdcKeyNames);
		context.putObject(CustomFieldsAdapter.OPTION_MDC_RETAINED_FIELDS, retainFieldMdcKeyNames);
        context.putObject(CustomFieldsAdapter.OPTION_SEND_DEFAULT_VALUES, sendDefaultValues);

		JsonLayout jsonLayout = new JsonLayout();
		jsonLayout.setContext(context);
		jsonLayout.start();

		layout = jsonLayout;
		super.start();
	}
}
