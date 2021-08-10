package com.sap.hcp.cf.logging.common.request;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Value;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

public class RequestRecordBuilder {

    private final RequestRecord requestRecord;

    private RequestRecordBuilder(String layerKey) {
        this.requestRecord = new RequestRecord(layerKey);
    }

    private RequestRecordBuilder(String layerKey, Direction direction) {
        this.requestRecord = new RequestRecord(layerKey, direction);
    }

    public static RequestRecordBuilder requestRecord(String layerKey) {
        return new RequestRecordBuilder(layerKey);
    }

    public static RequestRecordBuilder requestRecord(String layerKey, Direction direction) {
        return new RequestRecordBuilder(layerKey, direction);
    }

    public RequestRecord build() {
        return requestRecord;
    }

    public RequestRecordBuilder addTag(String fieldKey, String tag) {
        requestRecord.addTag(fieldKey, tag);
        return this;
    }

    public RequestRecordBuilder addOptionalTag(boolean optionalFieldCanBeLogged, String fieldKey, String tag) {

        if (!optionalFieldCanBeLogged && tag != null) {
            requestRecord.addTag(fieldKey, Defaults.REDACTED);
        }

        if (!optionalFieldCanBeLogged && Defaults.UNKNOWN.equals(tag)) {
            requestRecord.addTag(fieldKey, tag);
        }

        if (optionalFieldCanBeLogged) {
            requestRecord.addTag(fieldKey, tag);
        }
        return this;
    }

    public RequestRecordBuilder addContextTag(String fieldKey, String tag) {
        requestRecord.addContextTag(fieldKey, tag);
        return this;
    }

    public RequestRecordBuilder addValue(String fieldKey, Value value) {
        requestRecord.addValue(fieldKey, value);
        return this;
    }
}
