package com.sap.hcp.cf.logging.servlet.filter;

import java.io.PrintWriter;
import java.io.Writer;

public class WrappedPrintWriter extends PrintWriter {

    private static String LS_PROPERTY = "line.separator";
    private static String NEWLINE = "\n";
    private final Object lock;
    private long contentLength = -1;
    private String lineSeparator;

    public WrappedPrintWriter(Writer out) {
        super(out);
        lock = this;
        try {
            lineSeparator = System.getProperty(LS_PROPERTY, NEWLINE);
        } catch (Exception ex) {
            lineSeparator = NEWLINE;
        }
    }

    @Override
    public void write(int c) {
        synchronized (lock) {
            super.write(c);
            incrContentLength(1);
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        synchronized (lock) {
            super.write(buf, off, len);
            incrContentLength(len);
        }
    }

    @Override
    public void write(char[] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(String s, int off, int len) {
        synchronized (lock) {
            super.write(s, off, len);
            incrContentLength(len - off);
        }
    }

    @Override
    public void write(String s) {
        write(s, 0, s.length());
    }

    @Override
    public void println() {
        write(lineSeparator);
    }

    public long getContentLength() {
        return contentLength;
    }

    private void incrContentLength(int i) {
        /*
         * -- we wanted to be clever in previous versions and do a checkError()
         * first, -- but this has a nasty side-effect in that the underlying
         * buffer is flushed -- and thus we may run into IllegalStateExceptions
         */
        if (contentLength == -1) {
            contentLength = i;
        } else {
            contentLength += i;
        }
    }
}
