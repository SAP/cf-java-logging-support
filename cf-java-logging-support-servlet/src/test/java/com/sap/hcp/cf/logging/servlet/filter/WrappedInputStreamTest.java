package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import org.junit.Test;

public class WrappedInputStreamTest {

	private static final String MESSAGE = "ABCDEFGH";

	private static WrappedInputStream wrap(String text) {
		ByteArrayInputStream in = new ByteArrayInputStream(MESSAGE.getBytes(StandardCharsets.UTF_8));
		return new WrappedInputStream(new ServletInputStream() {

			@Override
			public int read() throws IOException {
				return in.read();
			}

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // nothing to do
            }
		});
	}

	@Test
	public void unreadInputGivesMinusOne() throws Exception {
		WrappedInputStream input = wrap(MESSAGE);

		assertThat(input.getContentLength(), is(equalTo(-1)));
	}

	@Test
	public void readSingleCharacter() throws Exception {
		WrappedInputStream input = wrap(MESSAGE);

		int read = input.read();

		assertThat((char) read, is(equalTo('A')));
		assertThat(input.getContentLength(), is(equalTo(1)));
	}

	@Test
	public void readCharacterArray() throws Exception {
		WrappedInputStream input = wrap(MESSAGE);
		byte[] cbuf = new byte[3];

		input.read(cbuf);

		assertThat(new String(cbuf), is(equalTo("ABC")));
		assertThat(input.getContentLength(), is(equalTo(3)));
	}

	@Test
	public void readCharacterArrayWithOffset() throws Exception {
		WrappedInputStream input = wrap(MESSAGE);

		byte[] cbuf = new byte[5];
		input.read(cbuf, 1, 4);

		byte[] expected = new byte[5];
		System.arraycopy(MESSAGE.getBytes(StandardCharsets.UTF_8), 0, expected, 1, 4);

		assertThat(cbuf, is(equalTo(expected)));
		assertThat(input.getContentLength(), is(equalTo(4)));
	}

	@Test
	public void skipCharacters() throws Exception {
		WrappedInputStream input = wrap(MESSAGE);

		long skipped = input.skip(3);

		assertThat(input.getContentLength(), is(equalTo((int) skipped)));
	}

}
