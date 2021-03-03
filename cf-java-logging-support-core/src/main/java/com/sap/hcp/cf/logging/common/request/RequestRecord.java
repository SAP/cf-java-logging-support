package com.sap.hcp.cf.logging.common.request;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.MDC;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.DoubleValue;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.LongValue;
import com.sap.hcp.cf.logging.common.StringValue;
import com.sap.hcp.cf.logging.common.Value;

/**
 * A <i>request record</i> keeping track of all fields defined for a request
 * message
 * <p>
 * Will "pretty-print" it's contents as a JSON object when serialized via
 * {@link #toString()}.
 * <p>
 * Will also take core to set some "defaults" and compute values if not
 * specified by the user. In a minimal usage scenario, i.e. without calling
 * {@link #addValue(String, Value)} or {@link #addTag(String, String)}, the
 * output will contain the following fields:
 * <ul>
 * <li><code>response_status_code</code> set to <code>200</code></li>
 * <li><code>response_payload_bytes</code> set to <code>-1</code></li>
 * <li><code>response_time_ms</code> set to the delta between the time when the
 * instance was created and the first call to {@link #toString()}</li>
 * </ul>
 * <p>
 * May "inherit" tags from other instances created in the same thread
 * (hierarchy) via (context) propagation, see
 * {@link RequestRecord#addContextTag(String, String)} Such context information
 * is kept in thread-local storage provided via {@link MDC}. As a consequence,
 * context information will stay until explicitly reset/cleared.
 *
 *
 */
public class RequestRecord {

    /*
     * -- default values for request fields that are marked as "required"
     */
    @SuppressWarnings("serial")
    private static Map<String, String> REQ_DEFAULTS = new HashMap<String, String>() {
        {
            put(Fields.REQUEST, Defaults.UNKNOWN);
            put(Fields.PROTOCOL, Defaults.UNKNOWN);
            put(Fields.METHOD, Defaults.UNKNOWN);
            put(Fields.REMOTE_IP, Defaults.UNKNOWN);
            put(Fields.REMOTE_HOST, Defaults.UNKNOWN);
            put(Fields.RESPONSE_CONTENT_TYPE, Defaults.UNKNOWN);
        }
    };

    public enum Direction {
                           IN, OUT
    }

    private Instant start;
    private Instant end;

    private Direction direction = Direction.IN;

    private final Map<String, Value> fields = new HashMap<String, Value>();
    private final Set<String> ctxFields = new HashSet<String>();

    /**
     * Creates a new instance that is tagged with the specified layer key. <br>
     * Implicitly sets defaults and the start time to "now" and direction to
     * "IN".
     * 
     * @param layerKey
     *            key to identify the layer that fills this instance
     */
    public RequestRecord(String layerKey) {
        this(layerKey, Direction.IN);
    }

    /**
     * Creates a new instance that is tagged with the specified layer key. <br>
     * Implicitly sets defaults and the start time to "now".
     * 
     * @param layerKey
     *            key to identify the layer that fills this instance
     * @param direction
     *            specifies the direction of the request
     */
    public RequestRecord(String layerKey, Direction direction) {
        addTag(Fields.LAYER, layerKey);
        if (direction == null) {
            direction = Direction.OUT;
        }
        this.direction = direction;
        addTag(Fields.DIRECTION, direction.toString());
        setDefaults();
        start();
    }

    /**
     * Add a non-null value for non-null key. <br>
     * If either key or value is <code>null</code>, nothing will happen.
     * 
     * @param key
     *            the key for which the value is stored
     * @param value
     *            the value to be stored
     * @return the former value stored for key (maybe <code>null</code>)
     */
    public Value addValue(String key, Value value) {
        if (key != null && value != null) {
            return fields.put(key, value);
        } else {
            return null;
        }
    }

    /**
     * Add a non-null tag for non-null key. <br>
     * If either key or value is <code>null</code>, nothing will happen.
     * 
     * @param key
     *            the key for which the tag will be stored
     * @param tag
     *            the tag to be stored
     * @return the former tag stored for key (maybe <code>null</code>)
     */
    public String addTag(String key, String tag) {
        if (key == null || tag == null) {
            return null;
        } else {
            Value oldValue = fields.put(key, new StringValue(tag));
            return oldValue != null ? oldValue.asString() : null;
        }
    }

    /**
     * Add a non-null tag for non-null key that should be "propagated" to
     * context. This is useful if more than one instance is created within a
     * thread (hierarchy) and subsequent instances are supposed to inherit tags
     * from earlier instances.
     * 
     * @param key
     *            the key for which the tag will be stored
     * @param tag
     *            the tag to be stored
     * @return the former tag stored for key (maybe <code>null</code>)
     */
    public String addContextTag(String key, String tag) {
        if (key == null || tag == null) {
            return null;
        } else {
            ctxFields.add(key);
            return LogContext.add(key, tag);
        }
    }

    public void resetContext() {
        LogContext.resetContextFields();
        for (String ctxField: ctxFields) {
            LogContext.remove(ctxField);
        }
        ctxFields.clear();
    }

    /**
     * Assigns the current time as the start time of this instance.
     * <p>
     * If newly assigned start time is later than current end time, end time
     * will be set to current time as well.
     * 
     * @return the assigned start time.
     */
    public long start() {
        start = Instant.now(ClockHolder.getInstance());
        return start.toEpochMilli();
    }

    /**
     * Assigns the current time as the end time of this instance.
     * 
     * @return the assigned end time.
     */
    public long stop() {
        end = Instant.now(ClockHolder.getInstance());
        return end.toEpochMilli();
    }

    @Override
    public String toString() {
        /*
         * -- make sure we have set start and end time
         */
        finish();
        try {
            JSONComposer<String> jc = JSON.std.composeString();
            ObjectComposer<JSONComposer<String>> oc = jc.startObject();
            for (Entry<String, Value> value: fields.entrySet()) {
                oc.putObject(value.getKey(), value.getValue().getValue());
            }
            return oc.end().finish();
        } catch (Exception ex) {
            return "{}";
        }
    }

    private void setDefaults() {
        LogContext.loadContextFields();
        addValue(Fields.RESPONSE_SIZE_B, Defaults.RESPONSE_SIZE_B);
        addValue(Fields.REQUEST_SIZE_B, Defaults.REQUEST_SIZE_B);
        addValue(Fields.RESPONSE_STATUS, Defaults.STATUS);
        for (Entry<String, String> tag: REQ_DEFAULTS.entrySet()) {
            addTag(tag.getKey(), tag.getValue());
        }
    }

    private void finish() {
        /*
         * -- We want to make sure that we have a valid response time -- If
         * response_time_ms has been explicitly set, we're done -- If not, make
         * sure, we stopped the timer and then compute the delta
         */
        if (!fields.containsKey(Fields.RESPONSE_TIME_MS)) {
            if (end == null) {
                stop();
            }
            setEndTimingTag(end.toString());
            addValue(Fields.RESPONSE_TIME_MS, new DoubleValue(Duration.between(start, end).toNanos() / 1_000_000.0));
        } else {
            Value respTime = fields.get(Fields.RESPONSE_TIME_MS);
            if (respTime != null) {
                long delta = 0;
                if (DoubleValue.class.isAssignableFrom(respTime.getClass())) {
                    delta = ((Double) respTime.getValue()).longValue();
                } else if (LongValue.class.isAssignableFrom(respTime.getClass())) {
                    delta = ((Long) respTime.getValue()).longValue();
                }
                setEndTimingTag(start.plusMillis(delta).toString());
            }
        }
        setStartTimingTag(start.toString());
    }

    private void setStartTimingTag(String dateValue) {
        String tag;
        if (direction == Direction.IN) {
            tag = Fields.REQUEST_RECEIVED_AT;
        } else {
            tag = Fields.REQUEST_SENT_AT;
        }
        addTag(tag, dateValue);

    }

    private void setEndTimingTag(String dateValue) {
        String tag;
        if (direction == Direction.IN) {
            tag = Fields.RESPONSE_SENT_AT;
        } else {
            tag = Fields.RESPONSE_RECEIVED_AT;
        }
        addTag(tag, dateValue);

    }

    static class ClockHolder {
        static Clock instance = Clock.systemUTC();

        public static Clock getInstance() {
            return ClockHolder.instance;
        }
    }
}
