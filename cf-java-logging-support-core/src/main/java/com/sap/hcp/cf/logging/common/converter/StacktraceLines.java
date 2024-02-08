package com.sap.hcp.cf.logging.common.converter;

import java.util.List;

public class StacktraceLines {

    private List<String> lines;

    public StacktraceLines(List<String> lines) {
        this.lines = lines;
    }

    public List<String> getLines() {
        return lines;
    }

    public int getTotalLineLength() {
        int length = 0;
        for (String line: lines) {
            length += line.length();
        }
        return length;
    }

    /**
     * Extracts the first n lines of the given stacktrace
     *
     * @param firstSize
     *         the number of lines to retain from start of the stacktrace
     * @return the first part of the stacktrace of the given size
     */
    public List<String> getFirstLines(int firstSize) {
        int size = 0;
        int i = 0;
        while (size < firstSize && i < lines.size()) {
            size += lines.get(i).length();
            i++;
        }
        return lines.subList(0, i - 1);
    }

    /**
     * Extracts the last n lines of the given stacktrace
     *
     * @param lastSize
     *         the number of lines to retain at the and of the stacktrace
     * @return the last part of the stacktrace of the given size
     */

    public List<String> getLastLines(int lastSize) {
        int size = 0;
        int i = lines.size() - 1;
        while (size < lastSize && i >= 0) {
            size += lines.get(i).length();
            i--;
        }
        return lines.subList(i + 2, lines.size());
    }

}
