package dev.kakrizky.lightwind.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

/**
 * JAX-RS filter that assigns a unique request ID (correlation ID) to each request.
 *
 * <p>If the incoming request already has an {@code X-Request-Id} header (e.g., from
 * an API gateway or load balancer), it is preserved. Otherwise, a new UUID is generated.</p>
 *
 * <p>The request ID is available via {@code RequestIdFilter.getCurrentRequestId()}
 * and is included in the response as {@code X-Request-Id} header.</p>
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
@ApplicationScoped
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_PROPERTY = "lightwind.request-id";

    private static final ThreadLocal<String> CURRENT_REQUEST_ID = new ThreadLocal<>();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        requestContext.setProperty(REQUEST_ID_PROPERTY, requestId);
        CURRENT_REQUEST_ID.set(requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String requestId = (String) requestContext.getProperty(REQUEST_ID_PROPERTY);
        if (requestId != null) {
            responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, requestId);
        }
        CURRENT_REQUEST_ID.remove();
    }

    /**
     * Get the request ID for the current thread/request.
     */
    public static String getCurrentRequestId() {
        return CURRENT_REQUEST_ID.get();
    }
}
