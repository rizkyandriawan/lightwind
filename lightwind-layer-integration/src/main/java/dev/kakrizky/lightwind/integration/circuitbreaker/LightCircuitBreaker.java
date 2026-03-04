package dev.kakrizky.lightwind.integration.circuitbreaker;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor binding that applies circuit breaker logic to a method.
 *
 * <p>When the circuit is open, the method will not be invoked and a
 * {@link CircuitBreakerOpenException} is thrown instead.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;LightCircuitBreaker(name = "payment-api", failureThreshold = 3)
 * public PaymentResult processPayment(PaymentRequest request) {
 *     return paymentClient.charge(request);
 * }
 * </pre>
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LightCircuitBreaker {

    /**
     * Circuit name. Defaults to the fully-qualified method name if empty.
     */
    @Nonbinding
    String name() default "";

    /**
     * Number of consecutive failures before the circuit opens.
     */
    @Nonbinding
    int failureThreshold() default 5;

    /**
     * Time in milliseconds before transitioning from OPEN to HALF_OPEN.
     */
    @Nonbinding
    long resetTimeoutMs() default 30000;
}
