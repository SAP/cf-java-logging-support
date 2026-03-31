package com.sap.hcp.cf.logging.servlet.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUriMatcherTest {

    @Test
    void doesNotMatchWhenNoPatternsConfigured() {
        RequestUriMatcher matcher = new RequestUriMatcher(null);
        assertThat(matcher.matches("/health")).isFalse();
    }

    @Test
    void doesNotMatchWhenPatternStringIsBlank() {
        RequestUriMatcher matcher = new RequestUriMatcher("   ");
        assertThat(matcher.matches("/health")).isFalse();
    }

    @Test
    void doesNotMatchNullUri() {
        RequestUriMatcher matcher = new RequestUriMatcher("/health");
        assertThat(matcher.matches(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/health", "/metrics", "/actuator" })
    void matchesExactPattern(String uri) {
        RequestUriMatcher matcher = new RequestUriMatcher("/health,/metrics,/actuator");
        assertThat(matcher.matches(uri)).isTrue();
    }

    @Test
    void doesNotMatchDifferentPath() {
        RequestUriMatcher matcher = new RequestUriMatcher("/health");
        assertThat(matcher.matches("/api/orders")).isFalse();
    }

    @Test
    void matchesSingleWildcardWithinSegment() {
        RequestUriMatcher matcher = new RequestUriMatcher("/api/*/status");
        assertThat(matcher.matches("/api/orders/status")).isTrue();
    }

    @Test
    void singleWildcardDoesNotMatchAcrossSegments() {
        RequestUriMatcher matcher = new RequestUriMatcher("/api/*/status");
        assertThat(matcher.matches("/api/orders/items/status")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "/actuator/**,  /actuator/health", "/actuator/**,  /actuator/health/liveness",
                 "/actuator/**,  /actuator/", "/api/**,       /api/v1/orders/123" })
    void matchesDoubleWildcardAcrossSegments(String pattern, String uri) {
        RequestUriMatcher matcher = new RequestUriMatcher(pattern.trim());
        assertThat(matcher.matches(uri.trim())).isTrue();
    }

    @Test
    void doubleWildcardDoesNotMatchSiblingPath() {
        RequestUriMatcher matcher = new RequestUriMatcher("/actuator/**");
        assertThat(matcher.matches("/api/orders")).isFalse();
    }

    @Test
    void matchesFirstOfMultiplePatterns() {
        RequestUriMatcher matcher = new RequestUriMatcher("/health, /actuator/**, /metrics");
        assertThat(matcher.matches("/health")).isTrue();
    }

    @Test
    void matchesMiddleOfMultiplePatterns() {
        RequestUriMatcher matcher = new RequestUriMatcher("/health, /actuator/**, /metrics");
        assertThat(matcher.matches("/actuator/health")).isTrue();
    }

    @Test
    void matchesLastOfMultiplePatterns() {
        RequestUriMatcher matcher = new RequestUriMatcher("/health, /actuator/**, /metrics");
        assertThat(matcher.matches("/metrics")).isTrue();
    }

    @Test
    void doesNotMatchWhenNoneOfMultiplePatternsApply() {
        RequestUriMatcher matcher = new RequestUriMatcher("/health, /actuator/**, /metrics");
        assertThat(matcher.matches("/api/orders")).isFalse();
    }

    @Test
    void ignoresWhitespaceAroundPatterns() {
        RequestUriMatcher matcher = new RequestUriMatcher("  /health  ,  /metrics  ");
        assertThat(matcher.matches("/health")).isTrue();
        assertThat(matcher.matches("/metrics")).isTrue();
    }
}
