package dev.kakrizky.lightwind.realtime.sse;

import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/**
 * Abstract base class for SSE endpoints.
 * <p>
 * Subclass in your application:
 * <pre>
 * {@literal @}Path("/api/events")
 * {@literal @}ApplicationScoped
 * public class EventSseResource extends LightSseResource {
 * }
 * </pre>
 */
public abstract class LightSseResource {

    @Inject
    SseConnectionManager sseConnectionManager;

    @Inject
    JsonWebToken jwt;

    @Inject
    Sse sse;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RolesAllowed("**")
    public Multi<OutboundSseEvent> stream() {
        UUID userId = extractUserId();
        authorize(userId);

        return sseConnectionManager.createStream(userId)
                .map(event -> sse.newEventBuilder()
                        .id(event.getId())
                        .name(event.getName())
                        .data(event.getData())
                        .reconnectDelay(event.getRetry() != null ? event.getRetry() : 300)
                        .build());
    }

    /**
     * Override to add custom authorization logic beyond JWT validation.
     */
    protected void authorize(UUID userId) {
        // Default: no additional auth
    }

    private UUID extractUserId() {
        String userId = jwt.getClaim("user_id");
        if (userId == null) {
            userId = jwt.getSubject();
        }
        return UUID.fromString(userId);
    }
}
