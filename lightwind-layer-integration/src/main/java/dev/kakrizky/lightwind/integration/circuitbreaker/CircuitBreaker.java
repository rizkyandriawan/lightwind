package dev.kakrizky.lightwind.integration.circuitbreaker;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Thread-safe circuit breaker implementation that tracks state per named circuit.
 *
 * <p>State transitions:</p>
 * <ul>
 *   <li>CLOSED to OPEN — after {@code failureThreshold} consecutive failures</li>
 *   <li>OPEN to HALF_OPEN — after {@code resetTimeoutMs} has elapsed</li>
 *   <li>HALF_OPEN to CLOSED — on a successful call</li>
 *   <li>HALF_OPEN to OPEN — on a failed call</li>
 * </ul>
 */
@ApplicationScoped
public class CircuitBreaker {

    private static final Logger LOG = Logger.getLogger(CircuitBreaker.class);

    private final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    /**
     * Executes an action through the named circuit breaker.
     *
     * @param name     the circuit name
     * @param action   the action to execute
     * @param fallback the fallback to execute when the circuit is open (may be null)
     * @param <T>      the return type
     * @return the action result, or fallback result if circuit is open
     * @throws CircuitBreakerOpenException if the circuit is open and no fallback is provided
     */
    public <T> T execute(String name, Supplier<T> action, Supplier<T> fallback) {
        return execute(name, CircuitBreakerConfig.defaults(), action, fallback);
    }

    /**
     * Executes an action through the named circuit breaker with custom configuration.
     *
     * @param name     the circuit name
     * @param config   the circuit configuration
     * @param action   the action to execute
     * @param fallback the fallback to execute when the circuit is open (may be null)
     * @param <T>      the return type
     * @return the action result, or fallback result if circuit is open
     * @throws CircuitBreakerOpenException if the circuit is open and no fallback is provided
     */
    public <T> T execute(String name, CircuitBreakerConfig config, Supplier<T> action, Supplier<T> fallback) {
        CircuitState state = circuits.computeIfAbsent(name, k -> new CircuitState(config));

        CircuitBreakerState currentState = evaluateState(name, state, config);

        if (currentState == CircuitBreakerState.OPEN) {
            LOG.debugf("Circuit '%s' is OPEN, rejecting call", name);
            if (fallback != null) {
                return fallback.get();
            }
            throw new CircuitBreakerOpenException(name);
        }

        if (currentState == CircuitBreakerState.HALF_OPEN) {
            int currentCalls = state.halfOpenCalls.incrementAndGet();
            if (currentCalls > config.getHalfOpenMaxCalls()) {
                LOG.debugf("Circuit '%s' HALF_OPEN max calls exceeded, rejecting", name);
                if (fallback != null) {
                    return fallback.get();
                }
                throw new CircuitBreakerOpenException(name);
            }
        }

        try {
            T result = action.get();
            onSuccess(name, state);
            return result;
        } catch (Exception e) {
            onFailure(name, state, config);
            throw e;
        }
    }

    private CircuitBreakerState evaluateState(String name, CircuitState state, CircuitBreakerConfig config) {
        CircuitBreakerState current = state.state.get();

        if (current == CircuitBreakerState.OPEN) {
            long elapsed = System.currentTimeMillis() - state.lastFailureTime.get();
            if (elapsed >= config.getResetTimeoutMs()) {
                if (state.state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                    state.halfOpenCalls.set(0);
                    LOG.infof("Circuit '%s' transitioned from OPEN to HALF_OPEN", name);
                }
                return CircuitBreakerState.HALF_OPEN;
            }
        }

        return state.state.get();
    }

    private void onSuccess(String name, CircuitState state) {
        CircuitBreakerState current = state.state.get();
        if (current == CircuitBreakerState.HALF_OPEN) {
            state.state.set(CircuitBreakerState.CLOSED);
            state.failureCount.set(0);
            state.halfOpenCalls.set(0);
            LOG.infof("Circuit '%s' transitioned from HALF_OPEN to CLOSED", name);
        } else {
            state.failureCount.set(0);
        }
    }

    private void onFailure(String name, CircuitState state, CircuitBreakerConfig config) {
        CircuitBreakerState current = state.state.get();
        state.lastFailureTime.set(System.currentTimeMillis());

        if (current == CircuitBreakerState.HALF_OPEN) {
            state.state.set(CircuitBreakerState.OPEN);
            state.halfOpenCalls.set(0);
            LOG.infof("Circuit '%s' transitioned from HALF_OPEN to OPEN", name);
        } else {
            int failures = state.failureCount.incrementAndGet();
            if (failures >= config.getFailureThreshold()) {
                state.state.set(CircuitBreakerState.OPEN);
                LOG.infof("Circuit '%s' transitioned from CLOSED to OPEN after %d failures", name, failures);
            }
        }
    }

    /**
     * Internal mutable state for a single named circuit.
     */
    private static class CircuitState {

        final AtomicReference<CircuitBreakerState> state;
        final AtomicInteger failureCount;
        final AtomicLong lastFailureTime;
        final AtomicInteger halfOpenCalls;

        CircuitState(CircuitBreakerConfig config) {
            this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
            this.failureCount = new AtomicInteger(0);
            this.lastFailureTime = new AtomicLong(0);
            this.halfOpenCalls = new AtomicInteger(0);
        }
    }
}
