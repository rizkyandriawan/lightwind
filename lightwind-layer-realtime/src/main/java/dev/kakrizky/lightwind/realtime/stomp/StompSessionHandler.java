package dev.kakrizky.lightwind.realtime.stomp;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StompSessionHandler {

    private final String connectionId;
    private UUID userId;
    private boolean authenticated;
    private final Set<String> subscriptionIds;

    public StompSessionHandler(String connectionId) {
        this.connectionId = connectionId;
        this.authenticated = false;
        this.subscriptionIds = ConcurrentHashMap.newKeySet();
    }

    public void markAuthenticated(UUID userId) {
        this.userId = userId;
        this.authenticated = true;
    }

    public void addSubscription(String subscriptionId) {
        subscriptionIds.add(subscriptionId);
    }

    public void removeSubscription(String subscriptionId) {
        subscriptionIds.remove(subscriptionId);
    }

    public String getConnectionId() {
        return connectionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Set<String> getSubscriptionIds() {
        return subscriptionIds;
    }
}
