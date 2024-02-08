package com.sap.hcp.cf.logging.common.serialization;

import com.fasterxml.jackson.jr.ob.JSON;
import com.sap.hcp.cf.logging.common.request.RequestRecord;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public abstract class AbstractRequestRecordFieldSupplier<T> implements EventContextFieldSupplier<T> {

    public AbstractRequestRecordFieldSupplier() {
        super();
    }

    @Override
    public Map<String, Object> map(T event) {
        if (!isRequestLog(event)) {
            return Collections.emptyMap();
        }
        Object[] parameterArray = getParameterArray(event);
        if (parameterArray == null || parameterArray.length == 0) {
            try {
                Map<String, Object> parsed = JSON.std.mapFrom(getFormattedMessage(event));
                return parsed != null ? parsed : Collections.emptyMap();
            } catch (IOException ignored) {
                return Collections.emptyMap();
            }
        }
        Optional<RequestRecord> requestRecord = findRequestRecord(parameterArray);
        if (requestRecord.isPresent()) {
            RequestRecord record = requestRecord.get();
            return record.getFields().entrySet().stream().collect(toMap(e -> e.getKey(), e -> e.getValue().getValue()));
        }
        return Collections.emptyMap();
    }

    private Optional<RequestRecord> findRequestRecord(Object[] parameterArray) {
        return Stream.of(parameterArray).filter(o -> o instanceof RequestRecord).map(o -> (RequestRecord) o)
                     .findFirst();
    }

    protected abstract boolean isRequestLog(T event);

    protected abstract String getFormattedMessage(T event);

    protected abstract Object[] getParameterArray(T event);

}
