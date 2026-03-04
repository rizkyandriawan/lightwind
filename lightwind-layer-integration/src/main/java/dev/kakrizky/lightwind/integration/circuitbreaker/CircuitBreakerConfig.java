package dev.kakrizky.lightwind.integration.circuitbreaker;

/**
 * Per-circuit configuration for the circuit breaker.
 */
public class CircuitBreakerConfig {

    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final int halfOpenMaxCalls;

    public CircuitBreakerConfig(int failureThreshold, long resetTimeoutMs, int halfOpenMaxCalls) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
    }

    /**
     * Creates a default configuration: 5 failures, 30s reset, 3 half-open calls.
     */
    public static CircuitBreakerConfig defaults() {
        return new CircuitBreakerConfig(5, 30000L, 3);
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public long getResetTimeoutMs() {
        return resetTimeoutMs;
    }

    public int getHalfOpenMaxCalls() {
        return halfOpenMaxCalls;
    }
}
