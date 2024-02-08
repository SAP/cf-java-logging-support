package com.sap.hcp.cf.logging.common.helper;

import com.fasterxml.jackson.jr.ob.JSON;
import com.sap.hcp.cf.logging.common.Fields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class ConsoleExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    private ConsoleOutput consoleOutput;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        consoleOutput = new ConsoleOutput();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        consoleOutput.clear();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(ConsoleOutput.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return consoleOutput;
    }

    public static class ConsoleOutput {

        private final PrintStream systemOut;
        private final PrintStream systemErr;
        private final CaptureOutputStream capturedOut;
        private final CaptureOutputStream capturedErr;

        private ConsoleOutput() {
            this.systemOut = System.out;
            this.systemErr = System.err;
            this.capturedOut = new CaptureOutputStream(systemOut);
            this.capturedErr = new CaptureOutputStream(systemErr);
            System.setOut(new PrintStream(capturedOut));
            System.setErr(new PrintStream(capturedErr));
        }

        public void clear() {
            System.setOut(systemOut);
            System.setErr(systemErr);
        }

        public List<Map<String, Object>> getAllEvents() {
            return capturedOut.copy.toString().lines().map(l -> {
                try {
                    return JSON.std.mapFrom(l);
                } catch (IOException e) {
                    return Map.ofEntries(entry("_unparsed", (Object) l));
                }
            }).collect(Collectors.toList());
        }

        public String getLastMessage() {
            return (String) getLastEventMap().get("msg");
        }

        private String getLastLine(CaptureOutputStream captured) {
            Optional<String> lastLine = captured.copy.toString().lines().reduce((first, second) -> second);
            if (lastLine.isEmpty()) {
                Assertions.fail("Expected at least one log message but found none.");
            }
            return lastLine.get();
        }

        public Map<String, Object> getLastEventMap() {
            try {
                return JSON.std.mapFrom(getLastLine(capturedOut));
            } catch (IOException cause) {
                Assertions.fail("Cannot unmarshall JSON log event.", cause);

            }
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        public List<String> getLastCategories() {
            return (List<String>) getLastEventMap().get(Fields.CATEGORIES);
        }
    }

    private static class CaptureOutputStream extends OutputStream {

        private final OutputStream wrapped;
        private final ByteArrayOutputStream copy = new ByteArrayOutputStream();

        private CaptureOutputStream(OutputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void write(int b) throws IOException {
            wrapped.write(b);
            copy.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrapped.write(b);
            copy.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrapped.write(b, off, len);
            copy.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            wrapped.flush();
            copy.flush();
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
            copy.close();
        }
    }
}
