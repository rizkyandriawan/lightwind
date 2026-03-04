package dev.kakrizky.lightwind.realtime.connection;

import java.util.UUID;

public class Subscription {

    private String subscriptionId;
    private String destination;
    private String connectionId;
    private UUID userId;

    public Subscription(String subscriptionId, String destination, String connectionId, UUID userId) {
        this.subscriptionId = subscriptionId;
        this.destination = destination;
        this.connectionId = connectionId;
        this.userId = userId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
