package dev.kakrizky.lightwind.realtime.stomp;

import dev.kakrizky.lightwind.exception.UnauthorizedException;
import dev.kakrizky.lightwind.realtime.RealtimeConfig;
import dev.kakrizky.lightwind.realtime.auth.WebSocketAuthenticator;
import dev.kakrizky.lightwind.realtime.connection.ConnectionManager;
import dev.kakrizky.lightwind.realtime.connection.RealtimeConnection;
import dev.kakrizky.lightwind.realtime.connection.Subscription;
import dev.kakrizky.lightwind.realtime.connection.SubscriptionRegistry;
import dev.kakrizky.lightwind.realtime.handler.MessageHandler;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/ws/stomp")
public class StompWebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(StompWebSocketEndpoint.class);

    @Inject
    StompFrameCodec codec;

    @Inject
    WebSocketAuthenticator authenticator;

    @Inject
    ConnectionManager connectionManager;

    @Inject
    SubscriptionRegistry subscriptionRegistry;

    @Inject
    RealtimeConfig config;

    @Inject
    Instance<MessageHandler> messageHandlers;

    private final ConcurrentHashMap<String, StompSessionHandler> sessions = new ConcurrentHashMap<>();

    @OnOpen
    void onOpen(WebSocketConnection connection) {
        String connId = connection.id();
        sessions.put(connId, new StompSessionHandler(connId));
        LOG.debugf("WebSocket opened: %s", connId);
    }

    @OnTextMessage
    void onMessage(WebSocketConnection connection, String message) {
        // Heartbeat: single newline
        if (message.equals("\n") || message.equals("\r\n")) {
            return;
        }

        StompFrame frame = codec.decode(message);
        if (frame == null) {
            LOG.warnf("Failed to parse STOMP frame from connection %s", connection.id());
            return;
        }

        StompSessionHandler session = sessions.get(connection.id());
        if (session == null) {
            return;
        }

        switch (frame.getCommand()) {
            case CONNECT, STOMP -> handleConnect(connection, session, frame);
            case SUBSCRIBE -> handleSubscribe(connection, session, frame);
            case UNSUBSCRIBE -> handleUnsubscribe(session, frame);
            case SEND -> handleSend(session, frame);
            case DISCONNECT -> handleDisconnect(connection, session, frame);
            default -> sendError(connection, "Unsupported command: " + frame.getCommand());
        }
    }

    @OnClose
    void onClose(WebSocketConnection connection) {
        String connId = connection.id();
        StompSessionHandler session = sessions.remove(connId);
        if (session != null) {
            subscriptionRegistry.unsubscribeAll(connId);
            connectionManager.removeConnection(connId);
            LOG.debugf("WebSocket closed: %s (user: %s)", connId,
                    session.isAuthenticated() ? session.getUserId() : "unauthenticated");
        }
    }

    @OnError
    void onError(WebSocketConnection connection, Throwable error) {
        LOG.warnf("WebSocket error on %s: %s", connection.id(), error.getMessage());
        sendError(connection, error.getMessage());
    }

    private void handleConnect(WebSocketConnection connection, StompSessionHandler session, StompFrame frame) {
        String token = frame.getHeader("Authorization");
        if (token == null) {
            token = frame.getHeader("passcode");
        }

        try {
            UUID userId = authenticator.authenticate(token);
            session.markAuthenticated(userId);

            RealtimeConnection realtimeConn = new RealtimeConnection(connection.id(), connection, userId);
            connectionManager.addConnection(connection.id(), realtimeConn);

            StompFrame connected = new StompFrame(StompCommand.CONNECTED);
            connected.setHeader("version", "1.2");
            connected.setHeader("heart-beat", config.heartbeatIntervalMs() + ",0");
            connected.setHeader("user-id", userId.toString());
            connection.sendTextAndAwait(codec.encode(connected));

            LOG.infof("STOMP CONNECTED: user %s on connection %s", userId, connection.id());
        } catch (UnauthorizedException e) {
            sendError(connection, "Authentication failed: " + e.getMessage());
            connection.close().subscribe().with(
                    v -> LOG.debugf("Closed unauthenticated connection %s", connection.id()),
                    t -> LOG.warnf("Error closing connection %s: %s", connection.id(), t.getMessage())
            );
        }
    }

    private void handleSubscribe(WebSocketConnection connection, StompSessionHandler session, StompFrame frame) {
        requireAuthenticated(session, connection);

        String subscriptionId = frame.getId();
        String destination = frame.getDestination();

        if (subscriptionId == null || destination == null) {
            sendError(connection, "SUBSCRIBE requires 'id' and 'destination' headers");
            return;
        }

        String resolved = resolveDestination(destination, session.getUserId());
        Subscription sub = new Subscription(subscriptionId, resolved, connection.id(), session.getUserId());
        subscriptionRegistry.subscribe(sub);
        session.addSubscription(subscriptionId);

        sendReceiptIfRequested(connection, frame);
        LOG.debugf("SUBSCRIBE: %s -> %s (user: %s)", subscriptionId, resolved, session.getUserId());
    }

    private void handleUnsubscribe(StompSessionHandler session, StompFrame frame) {
        String subscriptionId = frame.getId();
        if (subscriptionId != null) {
            subscriptionRegistry.unsubscribe(subscriptionId);
            session.removeSubscription(subscriptionId);
        }
    }

    private void handleSend(StompSessionHandler session, StompFrame frame) {
        if (!session.isAuthenticated()) {
            return;
        }

        String destination = frame.getDestination();
        if (destination == null) {
            return;
        }

        for (MessageHandler handler : messageHandlers) {
            if (destination.equals(handler.getDestination()) ||
                    matchesPattern(handler.getDestination(), destination)) {
                try {
                    handler.handle(session.getUserId(), destination, frame.getBody());
                } catch (Exception e) {
                    LOG.warnf("MessageHandler error for %s: %s", destination, e.getMessage());
                }
            }
        }
    }

    private void handleDisconnect(WebSocketConnection connection, StompSessionHandler session, StompFrame frame) {
        sendReceiptIfRequested(connection, frame);
        subscriptionRegistry.unsubscribeAll(connection.id());
        connectionManager.removeConnection(connection.id());
        sessions.remove(connection.id());
    }

    private void requireAuthenticated(StompSessionHandler session, WebSocketConnection connection) {
        if (!session.isAuthenticated()) {
            sendError(connection, "Not authenticated. Send CONNECT first.");
            throw new UnauthorizedException("Not authenticated");
        }
    }

    private String resolveDestination(String destination, UUID userId) {
        // /user/queue/foo -> /user/{userId}/queue/foo
        if (destination.startsWith("/user/") && !destination.matches("/user/[0-9a-f-]{36}/.*")) {
            return "/user/" + userId + destination.substring(5);
        }
        return destination;
    }

    private boolean matchesPattern(String pattern, String destination) {
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return destination.startsWith(prefix);
        }
        return false;
    }

    private void sendError(WebSocketConnection connection, String message) {
        StompFrame error = new StompFrame(StompCommand.ERROR);
        error.setHeader("message", message);
        error.setHeader("content-type", "text/plain");
        error.setBody(message);
        try {
            connection.sendTextAndAwait(codec.encode(error));
        } catch (Exception e) {
            LOG.warnf("Failed to send STOMP ERROR frame: %s", e.getMessage());
        }
    }

    private void sendReceiptIfRequested(WebSocketConnection connection, StompFrame frame) {
        String receipt = frame.getReceipt();
        if (receipt != null) {
            StompFrame receiptFrame = new StompFrame(StompCommand.RECEIPT);
            receiptFrame.setHeader("receipt-id", receipt);
            try {
                connection.sendTextAndAwait(codec.encode(receiptFrame));
            } catch (Exception e) {
                LOG.warnf("Failed to send STOMP RECEIPT: %s", e.getMessage());
            }
        }
    }
}
