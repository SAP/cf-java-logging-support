package com.sap.hcp.cf.logging.common;

import com.sap.hcp.cf.logging.common.helper.ConsoleExtension;
import com.sap.hcp.cf.logging.common.helper.ConsoleExtension.ConsoleOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

import java.util.UUID;

import static com.sap.hcp.cf.logging.common.LogContext.getCorrelationId;
import static com.sap.hcp.cf.logging.common.LogContext.initializeContext;
import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.assertLastEventFields;
import static com.sap.hcp.cf.logging.common.helper.ConsoleAssertions.assertLastEventMessage;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ConsoleExtension.class)
public class TestLoggerContext extends AbstractTest {

    private static final String TEST_VALUE = "test-value";

    @AfterEach
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void testGetFromContext() throws Exception {
        MDC.put(Fields.CORRELATION_ID, TEST_VALUE);
        assertThat(getCorrelationId()).isEqualTo(TEST_VALUE);
    }

    @Test
    public void testGenerateNewID() throws Exception {
        initializeContext();
        assertThat(MDC.get(Fields.CORRELATION_ID)).isNotBlank();
    }

    @Test
    public void testGenerateNewIDWhenPassingNull() throws Exception {
        initializeContext(null);
        assertThat(MDC.get(Fields.CORRELATION_ID)).isNotBlank();
    }

    @Test
    public void testDoesNotOverwriteSetID() throws Exception {
        initializeContext(TEST_VALUE);
        assertThat(MDC.get(Fields.CORRELATION_ID)).isEqualTo(TEST_VALUE);
    }

    @Test
    public void testCorrelationIdIsUUID() throws Exception {
        initializeContext();
        String generatedUUID = MDC.get(Fields.CORRELATION_ID);

        UUID parsedUUID = UUID.fromString(generatedUUID);
        assertThat(generatedUUID).isEqualTo(parsedUUID.toString());
    }

    @Test
    public void testGenerateEmitsLogMessage(ConsoleOutput console) throws Exception {
        initializeContext();

        assertLastEventMessage(console).containsSubsequence("generated new correlation id");
        assertLastEventFields(console).containsEntry(Fields.CORRELATION_ID, MDC.get(Fields.CORRELATION_ID));
    }
}
