package com.sap.hcp.cf.log4j2.layout;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ArrayComposer;
import com.fasterxml.jackson.jr.ob.comp.ComposerBase;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import com.sap.hcp.cf.log4j2.converter.api.Log4jContextFieldSupplier;
import com.sap.hcp.cf.log4j2.layout.suppliers.LogEventUtilities;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.converter.LineWriter;
import com.sap.hcp.cf.logging.common.converter.StacktraceLines;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldConverter;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplierServiceLoader;
import com.sap.hcp.cf.logging.common.serialization.JsonSerializationException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A {@link StringLayout} implementation that encodes an {@link LogEvent} as a JSON object.
 * <p>
 * Under the hood, it's using Jackson to serialize the logging event into JSON. The encoder can be confiugred in the
 * log4j.xml:<blockquote>
 *
 * <pre>
 * &lt;Appenders&gt;
 *   &lt;Console name="STDOUT-JSON" target="SYSTEM_OUT" follow="true"&gt;
 *     &lt;JsonPatternLayout /&gt;
 *   &lt;/Console&gt;
 * &lt;/Appenders&gt;
 * </pre>
 *
 * </blockquote> The layout can be customized by several xml elements. See the
 * annotations on the factory method {@link #createLayout}.
 */

@Plugin(name = "JsonPatternLayout", category = "Core", elementType = "Layout", printObject = true)
public final class JsonPatternLayout extends AbstractStringLayout {

    private static final String NEWLINE = "\n";
    private final ContextFieldConverter contextFieldConverter;
    private final List<Log4jContextFieldSupplier> log4jContextFieldSuppliers;
    private final List<ContextFieldSupplier> contextFieldSuppliers;
    private final int maxStacktraceSize;

    private final boolean sendDefaultValues;

    private final JSON json;

    private JsonPatternLayout(Charset charset, boolean sendDefaultValues, int maxStacktraceSize,
                              String jsonBuilderClass, CustomFieldElement[] customFieldMdcKeys,
                              Log4jContextFieldSupplierElement[] log4jContextFieldSupplierElements,
                              ContextFieldSupplierElement[] contextFieldSupplierElements) {
        super(charset);
        this.sendDefaultValues = sendDefaultValues;
        this.maxStacktraceSize = maxStacktraceSize > 0 ? maxStacktraceSize : 55 * 1024;
        this.contextFieldConverter = contextFieldConverter(sendDefaultValues, customFieldMdcKeys);
        this.json = createJson(jsonBuilderClass);
        this.log4jContextFieldSuppliers =
                getContextFieldSuppliers(log4jContextFieldSupplierElements, Log4jContextFieldSupplier.class);
        this.contextFieldSuppliers = getContextFieldSuppliers(contextFieldSupplierElements, ContextFieldSupplier.class);
    }

    private static <E extends ElementSupplier, S extends ContextFieldSupplier> List<S> getContextFieldSuppliers(
            E[] elements, Class<S> supplierClass) {
        Stream<S> configSuppliers = loadContextFieldSuppliersFromConfig(elements, createInstance(supplierClass));
        return ContextFieldSupplierServiceLoader.addFieldSuppliers(configSuppliers, supplierClass);
    }

    private static <E extends ElementSupplier, S extends ContextFieldSupplier> Stream<S> loadContextFieldSuppliersFromConfig(
            E[] elements, Function<E, S> mapper) {
        if (elements == null || elements.length == 0) {
            return Stream.empty();
        }
        return Stream.of(elements).map(mapper).filter(Objects::nonNull);
    }

    private static <T extends ElementSupplier, R extends ContextFieldSupplier> Function<T, R> createInstance(
            Class<R> clazz) {
        return e -> {
            try {
                return createInstance(e.getSupplierClass(), clazz);
            } catch (ReflectiveOperationException cause) {
                LOGGER.warn("Cannot register ContextFieldSupplier <" + clazz.getSimpleName() + ">.", cause);
                return null;
            }
        };
    }

    // for testing
    List<ContextFieldSupplier> getContextFieldSuppliers() {
        return contextFieldSuppliers;
    }

    // for testing
    List<Log4jContextFieldSupplier> getLog4jContextFieldSuppliers() {
        return log4jContextFieldSuppliers;
    }

    @PluginFactory
    public static JsonPatternLayout createLayout(@PluginAttribute(value = "charset") final Charset charset,
                                                 @PluginAttribute(
                                                         value = "sendDefaultValues") final boolean sendDefaultValues,
                                                 @PluginAttribute(
                                                         value = "maxStacktraceSize") final int maxStacktraceSize,
                                                 @PluginAttribute(value = "jsonBuilder") final String jsonBuilderClass,
                                                 @PluginElement(
                                                         value = "customField") CustomFieldElement[] customFieldMdcKeyNames,
                                                 @PluginElement(
                                                         value = "log4jContextFieldSupplier") Log4jContextFieldSupplierElement[] log4jContextFieldSupplierElements,
                                                 @PluginElement(
                                                         value = "contextFieldSupplier") ContextFieldSupplierElement[] contextFieldSupplierElements,
                                                 @PluginConfiguration final Configuration config) {
        return new JsonPatternLayout(charset, sendDefaultValues, maxStacktraceSize, jsonBuilderClass,
                                     customFieldMdcKeyNames, log4jContextFieldSupplierElements,
                                     contextFieldSupplierElements);
    }

    private static JSON createJson(String jsonBuilderClass) {
        if (jsonBuilderClass == null || jsonBuilderClass.isEmpty()) {
            return JSON.std;
        }
        try {

            return createInstance(jsonBuilderClass, JSON.Builder.class).build();
        } catch (ReflectiveOperationException cause) {
            LOGGER.warn("Cannot register JsonBuilder, using default.", cause);
            return JSON.std;
        }
    }

    private static ContextFieldConverter contextFieldConverter(boolean sendDefaultValues,
                                                               CustomFieldElement... customFieldMdcKeys) {
        List<String> customFieldMdcKeyNames = new ArrayList<>(customFieldMdcKeys.length);
        List<String> retainFieldMdcKeyNames = new ArrayList<>(customFieldMdcKeys.length);
        for (CustomFieldElement customField: customFieldMdcKeys) {
            customFieldMdcKeyNames.add(customField.getKey());
            if (customField.isRetainOriginal()) {
                retainFieldMdcKeyNames.add(customField.getKey());
            }
        }
        return new ContextFieldConverter(sendDefaultValues, customFieldMdcKeyNames, retainFieldMdcKeyNames);
    }

    private static <T> T createInstance(String className, Class<T> interfaceClass) throws ReflectiveOperationException {
        ClassLoader classLoader = JsonPatternLayout.class.getClassLoader();
        Class<? extends T> clazz = classLoader.loadClass(className).asSubclass(interfaceClass);
        return clazz.getDeclaredConstructor().newInstance();
    }

    @Override
    public String toSerializable(LogEvent event) {
        try (StringBuilderWriter writer = new StringBuilderWriter(getStringBuilder())) {
            ObjectComposer<JSONComposer<OutputStream>> oc = json.composeTo(writer).startObject();
            addMarkers(oc, event);
            Map<String, Object> contextFields = collectContextFields(event);
            contextFieldConverter.addContextFields(oc, contextFields);
            contextFieldConverter.addCustomFields(oc, contextFields);
            addStacktrace(oc, event);
            oc.end().finish();
            return writer.append(NEWLINE).toString();
        } catch (IOException | JsonSerializationException cause) {
            // Fallback to emit just the formatted message
            LOGGER.error("Conversion failed ", cause);
            return LogEventUtilities.getFormattedMessage(event);
        }
    }

    private <P extends ComposerBase> void addMarkers(ObjectComposer<P> oc, LogEvent event) throws IOException {
        if (sendDefaultValues || event.getMarker() != null) {
            ArrayComposer<ObjectComposer<P>> ac = oc.startArrayField(Fields.CATEGORIES);
            addMarker(ac, event.getMarker());
            ac.end();
        }
    }

    private <P extends ComposerBase> void addMarker(ArrayComposer<P> ac, org.apache.logging.log4j.Marker marker)
            throws IOException {
        if (marker == null) {
            return;
        }
        ac.add(marker.getName());
        if (marker.hasParents()) {
            for (org.apache.logging.log4j.Marker current: marker.getParents()) {
                addMarker(ac, current);
            }
        }
    }

    private Map<String, Object> collectContextFields(LogEvent event) {
        Map<String, Object> contextFields = new HashMap<>();
        contextFieldSuppliers.forEach(s -> contextFields.putAll(s.get()));
        log4jContextFieldSuppliers.forEach(s -> contextFields.putAll(s.map(event)));
        return contextFields;
    }

    private <P extends ComposerBase> void addStacktrace(ObjectComposer<P> oc, LogEvent event) throws IOException {
        if (event.getThrown() != null) {
            LineWriter lw = new LineWriter();
            event.getThrown().printStackTrace(new PrintWriter(lw));
            List<String> lines = lw.getLines();
            StacktraceLines stacktraceLines = new StacktraceLines(lines);
            ArrayComposer<ObjectComposer<P>> ac = oc.startArrayField(Fields.STACKTRACE);
            if (stacktraceLines.getTotalLineLength() <= maxStacktraceSize) {
                for (String line: stacktraceLines.getLines()) {
                    ac.add(line);
                }
            } else {
                ac.add("-------- STACK TRACE TRUNCATED --------");
                for (String line: stacktraceLines.getFirstLines(maxStacktraceSize / 3)) {
                    ac.add(line);
                }
                ac.add("-------- OMITTED --------");
                for (String line: stacktraceLines.getLastLines((maxStacktraceSize / 3) * 2)) {
                    ac.add(line);
                }
            }
            ac.end();
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

}
