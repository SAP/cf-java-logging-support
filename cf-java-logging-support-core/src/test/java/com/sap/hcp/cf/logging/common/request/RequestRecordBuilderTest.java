package com.sap.hcp.cf.logging.common.request;

import static com.sap.hcp.cf.logging.common.request.RequestRecordBuilder.requestRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import org.junit.jupiter.api.Test;

import com.sap.hcp.cf.logging.common.Defaults;

public class RequestRecordBuilderTest {

	@Test
	public void testAddingSingleActivatedOptionalTagToRequestRecord() throws JSONObjectException, IOException {
		boolean canBeLogged = true;
		String key = "TestKey";
		String tag = "TestTag";

		RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

		assertEquals(tag, getFieldFromRequestRecord(requestRecord, key));
	}

	@Test
	public void testAddingSingleForbiddenOptionalTagToRequestRecord() throws JSONObjectException, IOException {
		boolean canBeLogged = false;
		String key = "TestKey";
		String tag = "TestTag";

		RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

		assertEquals(Defaults.REDACTED, getFieldFromRequestRecord(requestRecord, key));
	}

	@Test
	public void testAddingSingleForbiddenOptionalNullTagToRequestRecord() throws JSONObjectException, IOException {
		boolean canBeLogged = false;
		String key = "TestKey";
		String tag = Defaults.UNKNOWN;

		RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

		assertEquals(Defaults.UNKNOWN, getFieldFromRequestRecord(requestRecord, key));
	}

	@Test
	public void testAddingSingleActivatedOptionalNullTagToRequestRecord() throws JSONObjectException, IOException {
		boolean canBeLogged = true;
		String key = "TestKey";
		String tag = Defaults.UNKNOWN;

		RequestRecord requestRecord = requestRecord("TEST").addOptionalTag(canBeLogged, key, tag).build();

		assertEquals(Defaults.UNKNOWN, getFieldFromRequestRecord(requestRecord, key));
	}

	private String getFieldFromRequestRecord(RequestRecord requestRecord, String key)
			throws JSONObjectException, IOException {
		return JSON.std.mapFrom(requestRecord.toString()).get(key).toString();
	}
}
