package dev.kakrizky.lightwind.realtime.connection;

import io.quarkus.websockets.next.WebSocketConnection;

import java.time.LocalDateTime;
import java.util.UUID;

public class RealtimeConnection {

    private String connectionId;
    private WebSocketConnection webSocketConnection;
    private UUID userId;
    private LocalDateTime connectedAt;

    public RealtimeConnection(String connectionId, WebSocketConnection webSocketConnection, UUID userId) {
        this.connectionId = connectionId;
        this.webSocketConnection = webSocketConnection;
        this.userId = userId;
        this.connectedAt = LocalDateTime.now();
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public WebSocketConnection getWebSocketConnection() {
        return webSocketConnection;
    }

    public void setWebSocketConnection(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
}
