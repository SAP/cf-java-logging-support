package com.sap.hcp.cf.logging.servlet.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class WrappedInputReaderTest {

	private static final String MESSAGE = "ABCDEFGH";

	private static WrappedInputReader wrap(String text) {
		return new WrappedInputReader(new BufferedReader(new StringReader(text)));
	}

	@Test
	public void unreadInputGivesMinusOne() throws Exception {
		WrappedInputReader reader = wrap(MESSAGE);

		assertThat(reader.getContentLength(), is(equalTo(-1)));
	}

	@Test
	public void readSingleCharacter() throws Exception {
		WrappedInputReader reader = wrap(MESSAGE);

		int read = reader.read();
		
		assertThat((char) read, is(equalTo('A')));
		assertThat(reader.getContentLength(), is(equalTo(1)));
	}

	@Test
	public void readCharacterArray() throws Exception {
		WrappedInputReader reader = wrap(MESSAGE);
		char[] cbuf = new char[3];

		reader.read(cbuf);

		assertThat(new String(cbuf), is(equalTo("ABC")));
		assertThat(reader.getContentLength(), is(equalTo(3)));
	}

	@Test
	public void readCharacterArrayWithOffset() throws Exception {
		WrappedInputReader reader = wrap(MESSAGE);

		char[] cbuf = new char[5];
		reader.read(cbuf, 1, 4);

		assertThat(cbuf, is(equalTo(new char[] { 0, 'A', 'B', 'C', 'D' })));
		assertThat(reader.getContentLength(), is(equalTo(4)));
	}


	@Test
	public void skipCharacters() throws Exception {
		WrappedInputReader reader = wrap(MESSAGE);

		long skipped = reader.skip(3);

		assertThat(reader.getContentLength(), is(equalTo((int) skipped)));
	}

	@Test
	public void markAndReset() throws Exception {
		WrappedInputReader reader = wrap(MESSAGE);

		reader.mark(1);
		int blind = reader.read();
		reader.reset();
		String text = consume(reader);

		assertThat((char) blind, is(equalTo('A')));
		assertThat(text, is(equalTo(MESSAGE)));
		assertThat(reader.getContentLength(), is(equalTo(MESSAGE.length())));
	}

	private String consume(WrappedInputReader reader) throws IOException {
		StringBuffer buffer = new StringBuffer();
		int current;
		while ((current = reader.read()) != -1) {
			buffer.append((char) current);
		}
		return buffer.toString();
	}

}
