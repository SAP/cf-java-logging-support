package com.sap.hcp.cf.logging.common;

import static com.sap.hcp.cf.logging.common.converter.UnmarshallUtilities.unmarshalCustomFields;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.fasterxml.jackson.jr.ob.JSON;

public abstract class AbstractTest {

    public static final String TEST_MESSAGE = "this is a test message";
	// see logback-test.xml for valid field keys
	public static final String CUSTOM_FIELD_KEY = "custom-field";
	public static final int CUSTOM_FIELD_INDEX = 0;
	public static final String TEST_FIELD_KEY = "test-field";
	public static final int TEST_FIELD_INDEX = 1;
	public static final String RETAINED_FIELD_KEY = "retained-field";
	public static final int RETAINED_FIELD_INDEX = 2;
    public static final String SOME_KEY = "some_key";
    public static final String SOME_VALUE = "some value";
    public static final String SOME_OTHER_VALUE = "some other value";
	public static final String HACK_ATTEMPT = "}{:\",\"";

    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private PrintStream stdout;
	private PrintStream stderr;

    @BeforeEach
    public void setupStreams() {
		stdout = System.out;
		stderr = System.err;
		System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void teardownStreams() {
		System.setOut(stdout);
		System.setErr(stderr);
    }

    protected String getMessage() {
        return getField("msg");
    }

    protected String getField(String fieldName) {
        try {
            return JSON.std.mapFrom(lastLine()).get(fieldName).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> getList(String fieldName) {
        try {
            return (List<String>) JSON.std.mapFrom(lastLine()).get(fieldName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String lastLine() {
        String[] lines = outContent.toString().split("\n");
        return lines[lines.length - 1];
    }

	protected Map<String, Object> getCustomField(String fieldName) throws Exception {
		List<Map<String, Object>> fields = unmarshalCustomFields(outContent.toString(),
				Fields.CUSTOM_FIELDS);
		for (Map<String, Object> field : fields) {
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
