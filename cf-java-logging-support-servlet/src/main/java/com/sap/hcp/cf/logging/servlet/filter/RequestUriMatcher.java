package com.sap.hcp.cf.logging.servlet.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Matches request URIs against a list of Ant-style path patterns.
 * <p>
 * Supported wildcards:
 * <ul>
 *     <li>{@code *}  &ndash; matches any sequence of characters within a single path segment (no slash)</li>
 *     <li>{@code **} &ndash; matches any sequence of characters across path segment boundaries (including slashes)</li>
 * </ul>
 */
public class RequestUriMatcher {

    private final List<String> patterns;

    /**
     * Creates a matcher from a comma-separated list of Ant-style path patterns.
     *
     * @param commaSeparatedPatterns
     *         comma-separated pattern string, may be {@code null} or blank
     */
    public RequestUriMatcher(String commaSeparatedPatterns) {
        if (commaSeparatedPatterns == null || commaSeparatedPatterns.isBlank()) {
            this.patterns = Collections.emptyList();
        } else {
            this.patterns = Arrays.stream(commaSeparatedPatterns.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                                  .toList();
        }
    }

    /**
     * Recursively matches {@code pattern} starting at {@code pi} (pattern index) against {@code uri} starting at
     * {@code ui} (uri index). No heap allocation occurs during matching.
     */
    private static boolean matches(String pattern, int pi, String uri, int ui) {
        while (pi < pattern.length()) {
            char pc = pattern.charAt(pi);
            if (pc == '*') {
                boolean doubleWildcard = pi + 1 < pattern.length() && pattern.charAt(pi + 1) == '*';
                if (doubleWildcard) {
                    // ** — skip the ** and try matching the rest from every position in uri
                    pi += 2;
                    if (pi == pattern.length()) {
                        return true; // ** at end matches everything remaining
                    }
                    for (int i = ui; i <= uri.length(); i++) {
                        if (matches(pattern, pi, uri, i)) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    // * — advance past * and try matching the rest from every position
                    // within the current segment (no slash crossing)
                    pi++;
                    if (pi == pattern.length()) {
                        return uri.indexOf('/', ui) == -1; // * at end matches rest of segment
                    }
                    for (int i = ui; i <= uri.length(); i++) {
                        if (i > ui && uri.charAt(i - 1) == '/') {
                            return false; // * cannot cross a slash
                        }
                        if (matches(pattern, pi, uri, i)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else {
                // literal character — must match exactly
                if (ui >= uri.length() || uri.charAt(ui) != pc) {
                    return false;
                }
                pi++;
                ui++;
            }
        }
        return ui == uri.length();
    }

    /**
     * Returns {@code true} if the given URI matches any of the configured patterns.
     */
    public boolean matches(String uri) {
        if (uri == null) {
            return false;
        }
        for (String pattern: patterns) {
            if (matches(pattern, 0, uri, 0)) {
                return true;
            }
        }
        return false;
    }
}
