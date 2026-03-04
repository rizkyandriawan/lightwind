package dev.kakrizky.lightwind.integration.circuitbreaker;

/**
 * Thrown when an action is attempted on a circuit that is in the {@link CircuitBreakerState#OPEN} state.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    private final String name;

    public CircuitBreakerOpenException(String name) {
        super("Circuit breaker '" + name + "' is open");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
