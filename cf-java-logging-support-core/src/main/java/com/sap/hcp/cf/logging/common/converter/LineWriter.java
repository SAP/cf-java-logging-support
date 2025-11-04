package com.sap.hcp.cf.logging.common.converter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

public class LineWriter extends Writer {

    StringWriter sw = new StringWriter();

    private final List<String> lines = new LinkedList<String>();

    public LineWriter() {
    }

    public List<String> getLines() {
        return lines;
    }

    /**
     * We have decided for our use-case to ignore offset and length and always write the whole String.
     */
    @Override
    public void write(String str, int off, int len) {
        if (str != null && !str.isBlank()) {
            lines.add(str);
        }
    }

    /**
     * We have decided for our use-case to ignore offset and length and always write the whole char array
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String str = String.valueOf(cbuf);
        write(str, off, len);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

}
