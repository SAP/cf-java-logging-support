package com.sap.hcp.cf.logging.common;

import com.fasterxml.jackson.jr.ob.JSON;
import org.junit.After;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static com.sap.hcp.cf.logging.common.converter.UnmarshallUtilities.unmarshalCustomFields;

public abstract class AbstractTest implements LogMessageConstants {

    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream stdout;
    private PrintStream stderr;

    @Before
    public void setupStreams() {
        stdout = System.out;
        stderr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void teardownStreams() {
        System.setOut(stdout);
        System.setErr(stderr);
    }

    protected String getMessage() {
        return getField("msg");
    }

    protected String getField(String fieldName) {
        try {
            return JSON.std.mapFrom(getLastLine()).get(fieldName).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> getList(String fieldName) {
        try {
            return (List<String>) JSON.std.mapFrom(getLastLine()).get(fieldName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String getLastLine() {
        String[] lines = outContent.toString().split("\n");
        return lines[lines.length - 1];
    }

    protected Map<String, Object> getCustomField(String fieldName) throws Exception {
        List<Map<String, Object>> fields = unmarshalCustomFields(outContent.toString(), Fields.CUSTOM_FIELDS);
        for (Map<String, Object> field: fields) {
            if (fieldName.equals(field.get("k"))) {
                return field;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMap(String fieldName) {
        try {
            return (Map<String, Object>) JSON.std.mapFrom(outContent.toString()).get(fieldName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected boolean hasField(String fieldName) {
        return getField(fieldName) != null;
    }
}
