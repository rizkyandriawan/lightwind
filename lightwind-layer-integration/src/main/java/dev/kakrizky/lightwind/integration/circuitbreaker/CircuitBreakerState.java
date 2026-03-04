package dev.kakrizky.lightwind.integration.circuitbreaker;

/**
 * Possible states for a circuit breaker.
 *
 * <ul>
 *   <li>{@link #CLOSED} — normal operation, requests pass through</li>
 *   <li>{@link #OPEN} — circuit tripped, requests are blocked</li>
 *   <li>{@link #HALF_OPEN} — trial period, limited requests allowed to test recovery</li>
 * </ul>
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
