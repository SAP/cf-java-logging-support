package com.sap.hcp.cf.logging.servlet.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(input.getContentLength()).isEqualTo(-1);
    }

    @Test
    public void readSingleCharacter() throws Exception {
        WrappedInputStream input = wrap(MESSAGE);

        char read = (char) input.read();

        assertThat(read).isEqualTo('A');
        assertThat(input.getContentLength()).isEqualTo(1);
    }

    @Test
    public void readCharacterArray() throws Exception {
        WrappedInputStream input = wrap(MESSAGE);
        byte[] cbuf = new byte[3];

        input.read(cbuf);

        assertThat(new String(cbuf)).isEqualTo("ABC");
        assertThat(input.getContentLength()).isEqualTo(3);
    }

    @Test
    public void readCharacterArrayWithOffset() throws Exception {
        WrappedInputStream input = wrap(MESSAGE);

        byte[] cbuf = new byte[5];
        input.read(cbuf, 1, 4);

        byte[] expected = new byte[5];
        System.arraycopy(MESSAGE.getBytes(StandardCharsets.UTF_8), 0, expected, 1, 4);

        assertThat(cbuf).isEqualTo(expected);
        assertThat(input.getContentLength()).isEqualTo(4);
    }

    @Test
    public void skipCharacters() throws Exception {
        WrappedInputStream input = wrap(MESSAGE);

        long skipped = input.skip(3);

        assertThat(input.getContentLength()).isEqualTo(skipped);
    }

}
