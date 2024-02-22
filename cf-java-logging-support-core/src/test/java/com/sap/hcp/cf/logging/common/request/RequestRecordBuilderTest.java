package com.sap.hcp.cf.logging.common.request;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.sap.hcp.cf.logging.common.Defaults;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestRecordBuilderTest {

    @Test
    public void testAddingSingleActivatedOptionalTagToRequestRecord() throws IOException {
        boolean canBeLogged = true;
        String key = "TestKey";
        String tag = "TestTag";

        RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

        assertEquals(tag, getFieldFromRequestRecord(requestRecord, key));
    }

    @Test
    public void testAddingSingleForbiddenOptionalTagToRequestRecord() throws IOException {
        boolean canBeLogged = false;
        String key = "TestKey";
        String tag = "TestTag";

        RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

        assertEquals(Defaults.REDACTED, getFieldFromRequestRecord(requestRecord, key));
    }

    @Test
    public void testAddingSingleForbiddenOptionalNullTagToRequestRecord() throws IOException {
        boolean canBeLogged = false;
        String key = "TestKey";
        String tag = Defaults.UNKNOWN;

        RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

        assertEquals(Defaults.UNKNOWN, getFieldFromRequestRecord(requestRecord, key));
    }

    @Test
    public void testAddingSingleActivatedOptionalNullTagToRequestRecord() throws IOException {
        boolean canBeLogged = true;
        String key = "TestKey";
        String tag = Defaults.UNKNOWN;

        RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

        assertEquals(Defaults.UNKNOWN, getFieldFromRequestRecord(requestRecord, key));
    }

    private String getFieldFromRequestRecord(RequestRecord requestRecord, String key)
            throws IOException {
        return JSON.std.mapFrom(requestRecord.toString()).get(key).toString();
    }
}
