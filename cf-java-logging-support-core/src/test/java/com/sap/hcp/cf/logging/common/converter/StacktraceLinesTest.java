package com.sap.hcp.cf.logging.common.converter;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class StacktraceLinesTest {

    final List<String> lines = new ArrayList<String>(Arrays.asList("this is the first line", "this is the second line",
                                                                   "this is the third line"));

    @Test
    public void testStacktraceLinesGetLines() {
        StacktraceLines stackTraceLines = new StacktraceLines(lines);
        List<String> extractedLines = stackTraceLines.getLines();
        assertThat(lines, equalTo(extractedLines));
    }

    @Test
    public void testStacktraceLinesGetFirstLines() {
        int maxSizeOfFirstPart = 25;
        StacktraceLines stackTraceLines = new StacktraceLines(this.lines);
        List<String> extractedLines = stackTraceLines.getFirstLines(maxSizeOfFirstPart);

        int size = 0;
        for (String line: extractedLines) {
            size += line.length();
        }
        assertThat(extractedLines, hasItem(containsString("this is the first line")));
        assertThat(extractedLines, not(hasItem(containsString("this is the second line"))));
        assertThat(size, lessThan(maxSizeOfFirstPart));
    }

    @Test
    public void testStacktraceLinesGetFirstLinesUnderestimatingSizeOfFirstLine() {
        int maxSizeOfFirstPart = 10;
        StacktraceLines stackTraceLines = new StacktraceLines(lines);
        List<String> extractedLines = stackTraceLines.getFirstLines(maxSizeOfFirstPart);
        assertThat(extractedLines.size(), equalTo(0));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testStacktraceLinesGetFirstLinesOverEstimatingTotalSizeOfLines() {
        int maxSizeOfFirstPart = 2500;
        StacktraceLines stackTraceLines = new StacktraceLines(lines);
        stackTraceLines.getFirstLines(maxSizeOfFirstPart);
    }

    @Test
    public void testStacktraceLinesGetLastLines() {
        int maxSizeOfLastPart = 25;
        StacktraceLines stackTraceLines = new StacktraceLines(lines);
        List<String> extractedLines = stackTraceLines.getLastLines(maxSizeOfLastPart);

        int size = 0;
        for (String line: extractedLines) {
            size += line.length();
        }
        assertThat(extractedLines, hasItem(containsString("this is the third line")));
        assertThat(extractedLines, not(hasItem(containsString("this is the second line"))));
        assertThat(size, lessThan(maxSizeOfLastPart));
    }

    @Test
    public void testStacktraceLinesGetLastLinesUnderestimatingSizeOfLastLine() {
        int maxSizeOfLastPart = 10;
        StacktraceLines stackTraceLines = new StacktraceLines(lines);
        List<String> extractedLines = stackTraceLines.getLastLines(maxSizeOfLastPart);
        assertThat(extractedLines.size(), equalTo(0));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testStacktraceLinesGetLastLinesOverEstimatingTotalSizeOfLines() {
        int maxSizeOfFirstPart = 2500;
        StacktraceLines stackTraceLines = new StacktraceLines(lines);
        stackTraceLines.getLastLines(maxSizeOfFirstPart);
    }

    @Test
    public void testStacktraceLinesGetTotalLineLength() {
        StacktraceLines lineWriter = new StacktraceLines(lines);
        assertThat(lineWriter.getTotalLineLength(), equalTo(67));
    }

    @Test
    public void testStacktraceLinesGetTotalLineLengthOnEmptyLines() {
        ArrayList<String> emptyLines = new ArrayList<String>();
        StacktraceLines lineWriter = new StacktraceLines(emptyLines);
        assertThat(lineWriter.getTotalLineLength(), equalTo(0));
    }
}
