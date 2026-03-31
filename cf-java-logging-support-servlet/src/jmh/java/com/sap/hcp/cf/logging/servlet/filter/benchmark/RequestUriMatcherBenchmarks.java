package com.sap.hcp.cf.logging.servlet.filter.benchmark;

import com.sap.hcp.cf.logging.servlet.filter.RequestUriMatcher;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class RequestUriMatcherBenchmarks {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        // --- matchers ---
        public final RequestUriMatcher noPatterns = new RequestUriMatcher(null);
        public final RequestUriMatcher exactSingle = new RequestUriMatcher("/health");
        public final RequestUriMatcher wildcardDouble = new RequestUriMatcher("/actuator/**");
        public final RequestUriMatcher wildcardSingle = new RequestUriMatcher("/api/*/status");
        public final RequestUriMatcher multiPattern =
                new RequestUriMatcher("/health, /metrics, /actuator/**, /readyz, /livez");

        // --- URIs ---
        public final String uriHealth = "/health";
        public final String uriApiOrders = "/api/orders";
        public final String uriActuatorDeep = "/actuator/health/liveness";
        public final String uriApiStatus = "/api/orders/status";
        public final String uriNoMatch = "/api/v1/orders/123/items";
    }

    /** Baseline: no patterns configured — fastest possible path. */
    @Benchmark
    public boolean noPatterns(BenchmarkState s) {
        return s.noPatterns.matches(s.uriApiOrders);
    }

    /** Single exact pattern, URI matches. */
    @Benchmark
    public boolean exactMatch(BenchmarkState s) {
        return s.exactSingle.matches(s.uriHealth);
    }

    /** Single exact pattern, URI does not match. */
    @Benchmark
    public boolean exactNoMatch(BenchmarkState s) {
        return s.exactSingle.matches(s.uriApiOrders);
    }

    /** Double-wildcard pattern (/actuator/**), deep URI matches. */
    @Benchmark
    public boolean doubleWildcardMatch(BenchmarkState s) {
        return s.wildcardDouble.matches(s.uriActuatorDeep);
    }

    /** Double-wildcard pattern (/actuator/**), URI does not match. */
    @Benchmark
    public boolean doubleWildcardNoMatch(BenchmarkState s) {
        return s.wildcardDouble.matches(s.uriNoMatch);
    }

    /** Single-segment wildcard ({@code /api/&#42;/status}), URI matches. */
    @Benchmark
    public boolean singleWildcardMatch(BenchmarkState s) {
        return s.wildcardSingle.matches(s.uriApiStatus);
    }

    /**
     * Realistic actuator-exclusion scenario: five patterns, URI matches the third one (/actuator/**) — worst-case
     * traversal through the list.
     */
    @Benchmark
    public boolean multiPatternMatch(BenchmarkState s) {
        return s.multiPattern.matches(s.uriActuatorDeep);
    }

    /**
     * Realistic actuator-exclusion scenario: five patterns, URI matches none — full list traversal.
     */
    @Benchmark
    public boolean multiPatternNoMatch(BenchmarkState s) {
        return s.multiPattern.matches(s.uriNoMatch);
    }
}
