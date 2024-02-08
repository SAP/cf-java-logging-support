package com.sap.hcp.cf.logging.common.converter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LineWriterTest {

    @Test
    public void fillLines() throws IOException {
        LineWriter lines = new LineWriter();
        lines.write("first line", 0, 10);
        lines.write("second line", 0, 11);
        lines.close();

        assertThat(lines.getLines()).containsExactly("first line", "second line");
    }

    @Test
    public void fillLinesWithIgnoredOffsetAndLength() throws IOException {
        LineWriter lines = new LineWriter();
        lines.write("first line", 5, 100);
        lines.write("second line", 5, 110);
        lines.close();
        assertThat(lines.getLines()).containsExactly("first line", "second line");
    }

    @Test
    public void readNonexistentEntry() throws IOException {
        assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> {
            LineWriter lines = new LineWriter();
            lines.close();
            lines.getLines().get(10);
        });
    }

    @Test
    public void printWriterTest() {
        LineWriter lineWriter = new LineWriter();
        PrintWriter printWriter = new PrintWriter(lineWriter);
        printWriter.print("this is the first line");
        printWriter.print("this is the second line");

        assertThat(lineWriter.getLines()).containsExactly("this is the first line", "this is the second line");
        printWriter.close();
    }

    @Test
    public void fillLinesWithCharArrays() throws IOException {
        LineWriter lines = new LineWriter();
        char[] firstLine = { 'f', 'i', 'r', 's', 't', ' ', 'l', 'i', 'n', 'e' };
        char[] secondLine = { 's', 'e', 'c', 'o', 'n', 'd', ' ', 'l', 'i', 'n', 'e' };
        lines.write(firstLine, 0, 10);
        lines.write(secondLine, 0, 11);
        lines.close();
        assertThat(lines.getLines()).containsExactly("first line", "second line");
    }

    @Test
    public void fillLinesWithCharArraysWithIgnoredOffsetAndLength() throws IOException {
        LineWriter lines = new LineWriter();
        char[] firstLine = { 'f', 'i', 'r', 's', 't', ' ', 'l', 'i', 'n', 'e' };
        char[] secondLine = { 's', 'e', 'c', 'o', 'n', 'd', ' ', 'l', 'i', 'n', 'e' };
        lines.write(firstLine, 5, 100);
        lines.write(secondLine, 5, 110);
        lines.close();
        assertThat(lines.getLines()).containsExactly("first line", "second line");
    }
}
