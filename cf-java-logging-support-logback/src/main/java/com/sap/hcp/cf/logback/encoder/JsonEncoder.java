package com.sap.hcp.cf.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Builder;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ArrayComposer;
import com.fasterxml.jackson.jr.ob.comp.ComposerBase;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import com.sap.hcp.cf.logback.converter.api.LogbackContextFieldSupplier;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.converter.LineWriter;
import com.sap.hcp.cf.logging.common.converter.StacktraceLines;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldConverter;
import com.sap.hcp.cf.logging.common.serialization.ContextFieldSupplier;
import com.sap.hcp.cf.logging.common.serialization.JsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * An {@link Encoder} implementation that encodes an {@link ILoggingEvent} as a JSON object.
 * <p>
 * Under the hood, it's using Jackson to serialize the logging event into JSON. The encoder can be confiugred in the
 * logback.xml:<blockquote>
 *
 * <pre>
 * &lt;appender name="STDOUT-JSON" class="ch.qos.logback.core.ConsoleAppender"&gt;
 *    &lt;encoder class="com.sap.hcp.cf.logback.encoder.JsonEncoder"/&gt;
 * &lt;/appender&gt;
 * </pre>
 *
 * </blockquote> The encoder can be customized by several xml elements. See the
 * Javadoc on the setter methods of this class.
 */
public class JsonEncoder extends EncoderBase<ILoggingEvent> {

    private static final String NEWLINE = "\n";
    private Charset charset = StandardCharsets.UTF_8;
    private final List<String> customFieldMdcKeyNames = new ArrayList<>();
    private final List<String> retainFieldMdcKeyNames = new ArrayList<>();
    private boolean sendDefaultValues = false;
    private List<LogbackContextFieldSupplier> logbackContextFieldSuppliers = new ArrayList<>();
    private List<ContextFieldSupplier> contextFieldSuppliers = new ArrayList<>();
    private int maxStacktraceSize = 55 * 1024;
    private JSON.Builder jsonBuilder = JSON.builder();
    private JSON json;
    private ContextFieldConverter contextFieldConverter;

    public JsonEncoder() {
        logbackContextFieldSuppliers.add(new BaseFieldSupplier());
        logbackContextFieldSuppliers.add(new EventContextFieldSupplier());
        logbackContextFieldSuppliers.add(new RequestRecordFieldSupplier());
    }

    /**
     * <p>
     * Adds a field to the "#cf" object in the generated output. If the log event contains this field, it will be put
     * into this nested object. It will not appear on top level in the generated JSON output, unless it is also added as
     * retained field. See {@link #addRetainFieldMdcKeyName(String)}.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <customFieldMdcKeyName>} in the logback.xml configuration
     * file.
     * </p>
     *
     * @param name
     *         the field name (key) to add as custom field
     */
    public void addCustomFieldMdcKeyName(String name) {
        customFieldMdcKeyNames.add(name);
    }

    /**
     * <p>
     * Retains a copy of a custom field added by {@link #addCustomFieldMdcKeyName(String)} at top level in the generated
     * JSON output. This is useful when sending logs to different backend systems, that expect custom fields at
     * different structures.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <retainFieldMdcKeyName>} in the logback.xml configuration
     * file.
     * </p>
     *
     * @param name
     *         the field name (key) to add as custom field
     */
    public void addRetainFieldMdcKeyName(String name) {
        retainFieldMdcKeyNames.add(name);
    }

    /**
     * <p>
     * Use the given charset for message creation. Defaults to utf8. Note, that Jackson is using utf8 by default
     * independently from this configuration. You need to change the JSON build with {@link #setJsonBuilder(String)} to
     * change the Jackson encoding.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <charset>} in the logback.xml configuration file.
     * </p>
     *
     * @param name
     *         the name of the charset to use
     */
    public void setCharset(String name) {
        try {
            this.charset = Charset.forName(name);
        } catch (IllegalArgumentException cause) {
            LoggerHolder.LOG.warn("Cannot set charset ''" + name + "''. Falling back to default.", cause);
        }
    }

    /**
     * <p>
     * Limit stacktraces to this number of characters. This is to prevent message sizes above the platform limit. The
     * default value is 55*1024. It should result in messages below 64k in size. The size should be adjusted to fit the
     * limit and the used fields of the messages. Stacktraces, that exceed the size will be shortened in the middle. Top
     * and bottom most lines will be retained.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <maxStacktraceSize>} in the logback.xml configuration
     * file.
     * </p>
     *
     * @param maxStacktraceSize
     *         the maximum number of characters to be allowed for stacktraces
     */
    public void setMaxStacktraceSize(int maxStacktraceSize) {
        this.maxStacktraceSize = maxStacktraceSize;
    }

    /**
     * <p>
     * Send default values. Fields with empty or default values, e.g. "-" for strings will not be added to the log
     * message. This behaviour can be changed to always emit all fields.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <sendDefaultValues>} in the logback.xml configuration
     * file.
     * </p>
     *
     * @param sendDefaultValues
     *         the maximum number of characters to be allowed for stacktraces
     */
    public void setSendDefaultValues(boolean sendDefaultValues) {
        this.sendDefaultValues = sendDefaultValues;
    }

    /**
     * <p>
     * Overwrites the default JsonBuilder with the given class. This can be used to modify the created JSON, e.g. with
     * custom escaping or encoding.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <jsonBuilder>} in the logback.xml configuration file.
     * </p>
     *
     * @param className
     *         the maximum number of characters to be allowed for stacktraces
     */
    public void setJsonBuilder(String className) {
        try {
            Builder builder = createInstance(className, JSON.Builder.class);
            this.jsonBuilder = builder;
        } catch (Exception cause) {
            LoggerHolder.LOG.warn("Cannot register JsonBuilder, using default.", cause);
        }
    }

    private <T> T createInstance(String className, Class<T> interfaceClass)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                   NoSuchMethodException, SecurityException, ClassNotFoundException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        Class<? extends T> clazz = classLoader.loadClass(className).asSubclass(interfaceClass);
        return clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * <p>
     * Use this class to extract context fields from the logback Logevents. The provided classes are applied in order.
     * Later instances can overwrite earlier generated fields. LogbackContextFieldSuppliers are executed after
     * ContextFieldSuppliers.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <logbackContextFieldSupplier>} in the logback.xml
     * configuration file.
     * </p>
     *
     * @param className
     *         the maximum number of characters to be allowed for stacktraces
     */
    public void addLogbackContextFieldSupplier(String className) {
        try {
            LogbackContextFieldSupplier instance = createInstance(className, LogbackContextFieldSupplier.class);
            logbackContextFieldSuppliers.add(instance);

        } catch (Exception cause) {
            LoggerHolder.LOG.warn("Cannot register LogbackContextFieldSupplier.", cause);
        }
    }

    /**
     * <p>
     * Use this class to to provide additional context fields, e.g. from the environment. The provided classes are
     * applied in order. Later instances can overwrite earlier generated fields. LogbackContextFieldSuppliers are
     * executed after ContextFieldSuppliers.
     * </p>
     * <p>
     * This method is called by Joran for the xml tag {@code <contextFieldSupplier>} in the logback.xml configuration
     * file.
     * </p>
     *
     * @param className
     *         the maximum number of characters to be allowed for stacktraces
     */
    public void addContextFieldSupplier(String className) {
        try {
            ContextFieldSupplier instance = createInstance(className, ContextFieldSupplier.class);
            contextFieldSuppliers.add(instance);

        } catch (Exception cause) {
            LoggerHolder.LOG.warn("Cannot register ContextFieldSupplier.", cause);
        }
    }

    @Override
    public void start() {
        this.json = new JSON(jsonBuilder);
        this.contextFieldConverter =
                new ContextFieldConverter(sendDefaultValues, customFieldMdcKeyNames, retainFieldMdcKeyNames);
        this.contextFieldSuppliers =
                ContextFieldSuppliersServiceLoader.addSpiContextFieldSuppliers(contextFieldSuppliers);
        this.logbackContextFieldSuppliers =
                ContextFieldSuppliersServiceLoader.addSpiLogbackContextFieldSuppliers(logbackContextFieldSuppliers);
        super.start();
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        return getJson(event).getBytes(charset);
    }

    private String getJson(ILoggingEvent event) {
        try (StringWriter writer = new StringWriter()) {
            ObjectComposer<JSONComposer<OutputStream>> oc = json.composeTo(writer).startObject();
            addMarkers(oc, event);
            Map<String, Object> contextFields = collectContextFields(event);
            contextFieldConverter.addContextFields(oc, contextFields);
            contextFieldConverter.addCustomFields(oc, contextFields);
            addStacktrace(oc, event);
            oc.end().finish();
            return writer.append(NEWLINE).toString();
        } catch (IOException | JsonSerializationException ex) {
            // Fallback to emit just the message
            LoggerHolder.LOG.error("Conversion failed ", ex);
            return event.getFormattedMessage() + NEWLINE;
        }
    }

    private <P extends ComposerBase> void addMarkers(ObjectComposer<P> oc, ILoggingEvent event) throws IOException {
        if (sendDefaultValues || event.getMarker() != null) {
            ArrayComposer<ObjectComposer<P>> ac = oc.startArrayField(Fields.CATEGORIES);
            addMarker(ac, event.getMarker());
            ac.end();
        }
    }

    private <P extends ComposerBase> void addMarker(ArrayComposer<P> ac, Marker parent) throws IOException {
        if (parent == null) {
            return;
        }
        if (parent.hasReferences()) {
            Iterator<Marker> current = parent.iterator();
            while (current.hasNext()) {
                addMarker(ac, current.next());
            }
        }
        ac.add(parent.getName());
    }

    private Map<String, Object> collectContextFields(ILoggingEvent event) {
        Map<String, Object> contextFields = new HashMap<>();
        contextFieldSuppliers.forEach(s -> contextFields.putAll(s.get()));
        logbackContextFieldSuppliers.forEach(s -> contextFields.putAll(s.map(event)));
        return contextFields;
    }

    private <P extends ComposerBase> void addStacktrace(ObjectComposer<P> oc, ILoggingEvent event) throws IOException {
        IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy != null && ThrowableProxy.class.isAssignableFrom(proxy.getClass())) {
            Throwable throwable = ((ThrowableProxy) proxy).getThrowable();
            LineWriter lw = new LineWriter();
            throwable.printStackTrace(new PrintWriter(lw));
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

    private static class LoggerHolder {
        static final Logger LOG = LoggerFactory.getLogger(LoggerHolder.class.getEnclosingClass());
    }

}
