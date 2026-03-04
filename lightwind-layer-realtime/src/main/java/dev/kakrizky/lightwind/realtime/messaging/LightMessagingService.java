package dev.kakrizky.lightwind.realtime.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kakrizky.lightwind.realtime.RealtimeConfig;
import dev.kakrizky.lightwind.realtime.connection.ConnectionManager;
import dev.kakrizky.lightwind.realtime.connection.Subscription;
import dev.kakrizky.lightwind.realtime.connection.SubscriptionRegistry;
import dev.kakrizky.lightwind.realtime.sse.SseConnectionManager;
import dev.kakrizky.lightwind.realtime.stomp.StompCommand;
import dev.kakrizky.lightwind.realtime.stomp.StompFrame;
import dev.kakrizky.lightwind.realtime.stomp.StompFrameCodec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class LightMessagingService {

    private static final Logger LOG = Logger.getLogger(LightMessagingService.class);

    @Inject
    ConnectionManager connectionManager;

    @Inject
    SubscriptionRegistry subscriptionRegistry;

    @Inject
    SseConnectionManager sseConnectionManager;

    @Inject
    StompFrameCodec codec;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RealtimeConfig config;

    /**
     * Send a message to a specific user on a given destination.
     * Delivers via both STOMP (if subscribed) and SSE (if connected).
     *
     * @param userId the target user
     * @param destination the queue/topic path (e.g., "/queue/easydoc/progress")
     * @param payload the message payload (will be serialized to JSON)
     */
    public void sendToUser(UUID userId, String destination, Object payload) {
        String json = serialize(payload);
        String fullDestination = "/user/" + userId + destination;

        // Send via STOMP
        Set<Subscription> subs = subscriptionRegistry.findSubscriptions(fullDestination);
        for (Subscription sub : subs) {
            sendStompMessage(sub, fullDestination, json);
        }

        // Send via SSE
        sseConnectionManager.sendEvent(userId, destination, json);

        LOG.debugf("Sent to user %s on %s (%d STOMP subs)", userId, destination, subs.size());
    }

    /**
     * Broadcast a message to all subscribers of a destination.
     *
     * @param destination the topic path (e.g., "/topic/announcements")
     * @param payload the message payload
     */
    public void broadcast(String destination, Object payload) {
        String json = serialize(payload);
        Set<Subscription> subs = subscriptionRegistry.findSubscriptions(destination);

        for (Subscription sub : subs) {
            sendStompMessage(sub, destination, json);
        }

        sseConnectionManager.broadcastEvent(destination, json);

        LOG.debugf("Broadcast to %s (%d STOMP subs)", destination, subs.size());
    }

    /**
     * Send to a specific destination (exact match, no user prefix).
     */
    public void sendToDestination(String destination, Object payload) {
        String json = serialize(payload);
        Set<Subscription> subs = subscriptionRegistry.findSubscriptions(destination);

        for (Subscription sub : subs) {
            sendStompMessage(sub, destination, json);
        }
    }

    /**
     * Check if a user has any active connections.
     */
    public boolean isUserOnline(UUID userId) {
        return !connectionManager.getConnectionsForUser(userId).isEmpty()
                || sseConnectionManager.hasActiveStream(userId);
    }

    private void sendStompMessage(Subscription sub, String destination, String jsonBody) {
        StompFrame message = new StompFrame(StompCommand.MESSAGE);
        message.setHeader("subscription", sub.getSubscriptionId());
        message.setHeader("message-id", UUID.randomUUID().toString());
        message.setHeader("destination", destination);
        message.setHeader("content-type", "application/json");
        message.setBody(jsonBody);

        connectionManager.getConnection(sub.getConnectionId()).ifPresent(conn -> {
            try {
                conn.getWebSocketConnection().sendTextAndAwait(codec.encode(message));
            } catch (Exception e) {
                LOG.warnf("Failed to send STOMP MESSAGE to connection %s: %s",
                        sub.getConnectionId(), e.getMessage());
            }
        });
    }

    private String serialize(Object payload) {
        if (payload instanceof String) {
            return (String) payload;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message payload", e);
        }
    }
}
