package com.sap.hcp.cf.logging.jersey.filter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import com.fasterxml.jackson.jr.ob.JSON;

public abstract class AbstractFilterTest extends JerseyTest {

    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void teardownStreams() {
        System.setOut(null);
        System.setErr(null);

    }

    protected String getField(String fieldName) {
        try {
            /* -- we may have more than one line, just take the last -- */
            return JSON.std.mapFrom(getLastLine()).get(fieldName).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected String getField(String fieldName, int i) {
        try {
            return JSON.std.mapFrom(getLine(i)).get(fieldName).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected int getLogSize() {
        return outContent.toString().split("\n").length;
    }

    private String getLastLine() {
        String[] lines = outContent.toString().split("\n");
        return lines[lines.length - 1];
    }

    public String getLine(int i) {
        String[] lines = outContent.toString().split("\n");
        return lines[i];
    }
}
