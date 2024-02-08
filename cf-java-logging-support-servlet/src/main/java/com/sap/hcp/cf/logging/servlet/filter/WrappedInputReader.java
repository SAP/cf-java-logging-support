package com.sap.hcp.cf.logging.servlet.filter;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class WrappedInputReader extends FilterReader {

    private int contentLength = -1;
    private int markContentLength = -1;

    protected WrappedInputReader(Reader in) {
        super(in);
    }

    private static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    public int getContentLength() {
        return contentLength;
    }

    @Override
    public int read() throws IOException {
        int c = in.read();
        if (c != -1) {
            incrContentLength(1);
        }
        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int c = in.read(cbuf, off, len);
        if (c != -1) {
            incrContentLength(c);
        }
        return c;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = in.skip(n);
        incrContentLength(toIntExact(skipped));
        return skipped;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        super.mark(readAheadLimit);
        synchronized (lock) {
            this.markContentLength = contentLength;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        synchronized (lock) {
            this.contentLength = markContentLength;
        }
    }

    private void incrContentLength(int i) {
        if (contentLength == -1) {
            contentLength = i;
        } else {
            contentLength += i;
        }
    }
}
