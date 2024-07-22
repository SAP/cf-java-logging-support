package com.sap.hcp.cf.logging.common.converter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StacktraceLinesTest {

    private static final List<String> LINES = new ArrayList<String>(
            Arrays.asList("this is the first line", "this is the second line", "this is the third line"));

    @Test
    public void testStacktraceLinesGetLines() {
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        assertThat(stackTraceLines.getLines()).containsExactlyElementsOf(LINES);
    }

    @Test
    public void testStacktraceLinesGetFirstLines() {
        int maxSizeOfFirstPart = 25;
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        List<String> extractedLines = stackTraceLines.getFirstLines(maxSizeOfFirstPart);

        int size = extractedLines.stream().mapToInt(String::length).sum();

        assertThat(extractedLines).contains("this is the first line");
        assertThat(extractedLines).doesNotContain("this is the second line");
        assertThat(size).isLessThan(maxSizeOfFirstPart);
    }

    @Test
    public void testStacktraceLinesGetFirstLinesUnderestimatingSizeOfFirstLine() {
        int maxSizeOfFirstPart = 10;
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        List<String> extractedLines = stackTraceLines.getFirstLines(maxSizeOfFirstPart);
        assertThat(extractedLines).hasSize(0);
    }

    @Test
    public void testStacktraceLinesGetFirstLinesOverEstimatingTotalSizeOfLines() {
        int maxSizeOfFirstPart = 2500;
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        stackTraceLines.getFirstLines(maxSizeOfFirstPart);
        assertThat(stackTraceLines.getLines()).containsExactlyElementsOf(LINES);
    }

    @Test
    public void testStacktraceLinesGetLastLines() {
        int maxSizeOfLastPart = 25;
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        List<String> extractedLines = stackTraceLines.getLastLines(maxSizeOfLastPart);

        int size = extractedLines.stream().mapToInt(String::length).sum();
        assertThat(extractedLines).contains("this is the third line");
        assertThat(extractedLines).doesNotContain("this is the second line");
        assertThat(size).isLessThan(maxSizeOfLastPart);
    }

    @Test
    public void testStacktraceLinesGetLastLinesUnderestimatingSizeOfLastLine() {
        int maxSizeOfLastPart = 10;
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        List<String> extractedLines = stackTraceLines.getLastLines(maxSizeOfLastPart);
        assertThat(extractedLines).hasSize(0);
    }

    @Test
    public void testStacktraceLinesGetLastLinesOverEstimatingTotalSizeOfLines() {
        int maxSizeOfFirstPart = 2500;
        StacktraceLines stackTraceLines = new StacktraceLines(LINES);
        stackTraceLines.getLastLines(maxSizeOfFirstPart);
        assertThat(stackTraceLines.getLines()).containsExactlyElementsOf(LINES);
    }

    @Test
    public void testStacktraceLinesGetTotalLineLength() {
        StacktraceLines lineWriter = new StacktraceLines(LINES);
        assertThat(lineWriter.getTotalLineLength()).isEqualTo(67);
    }

    @Test
    public void testStacktraceLinesGetTotalLineLengthOnEmptyLines() {
        ArrayList<String> emptyLines = new ArrayList<String>();
        StacktraceLines lineWriter = new StacktraceLines(emptyLines);
        assertThat(lineWriter.getTotalLineLength()).isEqualTo(0);
    }
}
