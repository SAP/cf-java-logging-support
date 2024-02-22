package com.sap.hcp.cf.logging.servlet.filter;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

public class WrappedInputReaderTest {

    private static final String MESSAGE = "ABCDEFGH";

    private static WrappedInputReader wrap(String text) {
        return new WrappedInputReader(new BufferedReader(new StringReader(text)));
    }

    @Test
    public void unreadInputGivesMinusOne() throws Exception {
        WrappedInputReader reader = wrap(MESSAGE);

        assertThat(reader.getContentLength()).isEqualTo(-1);
    }

    @Test
    public void readSingleCharacter() throws Exception {
        WrappedInputReader reader = wrap(MESSAGE);

        assertThat((char) reader.read()).isEqualTo('A');
        assertThat(reader.getContentLength()).isEqualTo(1);
    }

    @Test
    public void readCharacterArray() throws Exception {
        WrappedInputReader reader = wrap(MESSAGE);
        char[] cbuf = new char[3];

        reader.read(cbuf);

        assertThat(new String(cbuf)).isEqualTo("ABC");
        assertThat(reader.getContentLength()).isEqualTo(3);
    }

    @Test
    public void readCharacterArrayWithOffset() throws Exception {
        WrappedInputReader reader = wrap(MESSAGE);

        char[] cbuf = new char[5];
        reader.read(cbuf, 1, 4);

        assertThat(cbuf).containsExactlyInAnyOrder((char) 0, 'A', 'B', 'C', 'D');
        assertThat(reader.getContentLength()).isEqualTo(4);
    }

    @Test
    public void skipCharacters() throws Exception {
        WrappedInputReader reader = wrap(MESSAGE);

        long skipped = reader.skip(3);

        assertThat(reader.getContentLength()).isEqualTo(skipped);
    }

    @Test
    public void markAndReset() throws Exception {
        WrappedInputReader reader = wrap(MESSAGE);

        reader.mark(1);
        int blind = reader.read();
        reader.reset();

        assertThat((char) blind).isEqualTo('A');
        assertThat(consume(reader)).isEqualTo(MESSAGE);
        assertThat(reader.getContentLength()).isEqualTo(MESSAGE.length());
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
