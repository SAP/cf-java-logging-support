package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * A simple response wrapper implementation that tries to determine the amount of data written to the client by wrapping
 * the output stream or the print writer.
 */
public class ContentLengthTrackingResponseWrapper extends HttpServletResponseWrapper {

    private final HttpServletResponse response;
    private WrappedOutputStream wrappedOS = null;
    private WrappedPrintWriter wrappedWriter = null;

    public ContentLengthTrackingResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (wrappedOS == null) {
            wrappedOS = new WrappedOutputStream(response.getOutputStream());
        }
        return wrappedOS;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (wrappedWriter == null) {
            wrappedWriter = new WrappedPrintWriter(response.getWriter());
        }
        return wrappedWriter;
    }

    public long getContentLength() {
        if (wrappedWriter != null) {
            return wrappedWriter.getContentLength();
        } else {
            if (wrappedOS != null) {
                return wrappedOS.getContentLength();
            } else {
                return -1;
            }
        }
    }
}
