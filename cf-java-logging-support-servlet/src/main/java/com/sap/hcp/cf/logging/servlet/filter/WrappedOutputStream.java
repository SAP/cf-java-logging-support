package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;

public class WrappedOutputStream extends ServletOutputStream {

    private final ServletOutputStream wrappedStream;
    private long contentLength = -1;

    public WrappedOutputStream(ServletOutputStream out) {
        wrappedStream = out;
    }

    public long getContentLength() {
        return contentLength;
    }

    @Override
    public void write(int b) throws IOException {
        wrappedStream.write(b);
        incrContentLength(1);
    }

    private void incrContentLength(int i) {
        if (contentLength == -1) {
            contentLength = i;
        } else {
            contentLength += i;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrappedStream.write(b);
        incrContentLength(b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrappedStream.write(b, off, len);
        incrContentLength(len);
    }

    @Override
    public void close() throws IOException {
        wrappedStream.close();
    }

    @Override
    public void flush() throws IOException {
        wrappedStream.flush();
    }

    @Override
    public boolean isReady() {
        return wrappedStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        wrappedStream.setWriteListener(writeListener);

    }
}
