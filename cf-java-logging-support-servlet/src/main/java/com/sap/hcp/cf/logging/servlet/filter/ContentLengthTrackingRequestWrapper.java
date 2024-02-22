package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 */
public class ContentLengthTrackingRequestWrapper extends HttpServletRequestWrapper {

    private WrappedInputReader wrappedReader = null;
    private WrappedInputStream wrappedStream = null;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException
     *         if the request is null
     */
    public ContentLengthTrackingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        wrappedStream = new WrappedInputStream(super.getInputStream());
        return wrappedStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        wrappedReader = new WrappedInputReader(super.getReader());
        return new BufferedReader(wrappedReader);
    }

    @Override
    public int getContentLength() {
        if (wrappedReader != null) {
            return wrappedReader.getContentLength();
        } else {
            if (wrappedStream != null) {
                return wrappedStream.getContentLength();
            } else {
                return -1;
            }
        }
    }
}
