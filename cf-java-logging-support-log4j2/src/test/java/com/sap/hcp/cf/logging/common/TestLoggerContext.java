package com.sap.hcp.cf.logging.common;

import static com.sap.hcp.cf.logging.common.LogContext.getCorrelationId;
import static com.sap.hcp.cf.logging.common.LogContext.initializeContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TestLoggerContext extends AbstractTest {

	public static final String MDC_KEY = "correlation_id";
	public static final String TEST_VALUE = "test-value";

	@Test
	public void testGetFromContext() throws Exception {
		setInMDC(TEST_VALUE);
		assertThat(getCorrelationId(), is(TEST_VALUE));
	}

	@Test
	public void testGenerateNewID() throws Exception {
		initializeContext();
		assertThat(getFromMDC(), not(isEmptyOrNullString()));
	}

	@Test
	public void testGenerateNewIDWhenPassingNull() throws Exception {
		initializeContext(null);
		assertThat(getFromMDC(), not(isEmptyOrNullString()));
	}

	@Test
	public void testDoesNotOverwriteSetID() throws Exception {
		initializeContext(TEST_VALUE);
		assertThat(getFromMDC(), is(TEST_VALUE));
	}

	@Test
	public void testCorrelationIdIsUUID() throws Exception {
		initializeContext();
		String generatedUUID = getFromMDC();

		UUID parsedUUID = UUID.fromString(generatedUUID);
		assertThat(parsedUUID.toString(), is(generatedUUID));
	}

	@Test
	public void testGenerateEmitsLogMessage() throws Exception {
		System.setErr(new PrintStream(new ByteArrayOutputStream()));

		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));

		initializeContext();
		String logMessage = outContent.toString();

		assertThat(logMessage, containsString("generated new correlation id"));
		assertThat(logMessage, containsString(getFromMDC()));
	}

	private String getFromMDC() {
		return MDC.get(MDC_KEY);
	}

	private void setInMDC(String expectedValue) {
		MDC.put(MDC_KEY, expectedValue);
	}
}
