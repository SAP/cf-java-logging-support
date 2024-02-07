package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;

/**
 *
 */
public class WrappedInputStream extends ServletInputStream {

	private int contentLength = -1;
	private int markContentLength = -1;
	private final ServletInputStream wrappedStream;

	public int getContentLength() {
		return contentLength;
	}

	protected WrappedInputStream(ServletInputStream in) {
		wrappedStream = in;
	}

	@Override
	public int read() throws IOException {
		int c = wrappedStream.read();
		if (c != -1) {
			incrContentLength(1);
		}
		return c;
	}

	@Override
	public int readLine(byte[] b, int off, int len) throws IOException {
		int retLen = wrappedStream.readLine(b, off, len);
		if (retLen != -1) {
			incrContentLength(retLen);
		}
		return retLen;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int len = wrappedStream.read(b);
		if (len != -1) {
			incrContentLength(len);
		}
		return len;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int c = wrappedStream.read(b, off, len);
        if (c != -1) {
            incrContentLength(c);
        }
        return c;
 	}

	private void incrContentLength(int i) {
		if (contentLength == -1) {
			contentLength = i;
		} else {
			contentLength += i;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = wrappedStream.skip(n);
		incrContentLength(toIntExact(skipped));
		return skipped;
	}

	private static int toIntExact(long value) {
		if ((int) value != value) {
			throw new ArithmeticException("integer overflow");
		}
		return (int) value;
	}

	@Override
	public int available() throws IOException {
		return wrappedStream.available();
	}

	@Override
	public void close() throws IOException {
		wrappedStream.close();
	}

	@Override
	public synchronized void mark(int readAheadLimit) {
		wrappedStream.mark(readAheadLimit);
		this.markContentLength = this.contentLength;
	}

	@Override
	public synchronized void reset() throws IOException {
		wrappedStream.reset();
		this.contentLength = this.markContentLength;
	}

	@Override
	public boolean markSupported() {
		return wrappedStream.markSupported();
	}

    @Override
    public boolean isFinished() {
        return wrappedStream.isFinished();
    }

    @Override
    public boolean isReady() {
        return wrappedStream.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        wrappedStream.setReadListener(readListener);
    }
}
