package dev.kakrizky.lightwind.realtime.handler;

import java.util.UUID;

/**
 * Interface for handling inbound STOMP SEND messages from clients.
 * <p>
 * Implement in your application:
 * <pre>
 * {@literal @}ApplicationScoped
 * public class ChatMessageHandler implements MessageHandler {
 *     public String getDestination() { return "/app/chat.send"; }
 *     public void handle(UUID userId, String destination, String payload) {
 *         // process the message
 *     }
 * }
 * </pre>
 */
public interface MessageHandler {

    /**
     * The STOMP destination this handler responds to.
     * Supports trailing wildcard: "/app/chat/*" matches "/app/chat/send".
     */
    String getDestination();

    /**
     * Handle an inbound message.
     *
     * @param userId the authenticated user who sent the message
     * @param destination the actual destination path
     * @param payload the message body (JSON string)
     */
    void handle(UUID userId, String destination, String payload);
}
