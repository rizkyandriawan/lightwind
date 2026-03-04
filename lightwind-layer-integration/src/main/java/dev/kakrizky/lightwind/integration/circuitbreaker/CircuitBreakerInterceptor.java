package dev.kakrizky.lightwind.integration.circuitbreaker;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;

/**
 * CDI interceptor that applies circuit breaker logic to methods annotated with
 * {@link LightCircuitBreaker}.
 *
 * <p>Extracts the circuit configuration from the annotation and delegates execution
 * to the {@link CircuitBreaker} service.</p>
 */
@LightCircuitBreaker
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class CircuitBreakerInterceptor {

    private static final Logger LOG = Logger.getLogger(CircuitBreakerInterceptor.class);

    @Inject
    CircuitBreaker circuitBreaker;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        Method method = ctx.getMethod();
        LightCircuitBreaker annotation = method.getAnnotation(LightCircuitBreaker.class);
        if (annotation == null) {
            return ctx.proceed();
        }

        String circuitName = annotation.name();
        if (circuitName == null || circuitName.isEmpty()) {
            circuitName = method.getDeclaringClass().getName() + "." + method.getName();
        }

        CircuitBreakerConfig config = new CircuitBreakerConfig(
                annotation.failureThreshold(),
                annotation.resetTimeoutMs(),
                3 // default halfOpenMaxCalls
        );

        LOG.debugf("Circuit breaker '%s' intercepting method %s", circuitName, method.getName());

        try {
            return circuitBreaker.execute(circuitName, config, () -> {
                try {
                    return ctx.proceed();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, null);
        } catch (CircuitBreakerOpenException e) {
            throw e;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }
}
