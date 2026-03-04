package dev.kakrizky.lightwind.realtime.connection;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class SubscriptionRegistry {

    private static final Logger LOG = Logger.getLogger(SubscriptionRegistry.class);

    private final ConcurrentHashMap<String, Set<Subscription>> destinationSubs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Subscription> allSubs = new ConcurrentHashMap<>();

    public void subscribe(Subscription sub) {
        allSubs.put(sub.getSubscriptionId(), sub);
        destinationSubs.computeIfAbsent(sub.getDestination(), k -> ConcurrentHashMap.newKeySet())
                .add(sub);
        LOG.debugf("Subscribed %s to %s (connection: %s)", sub.getSubscriptionId(), sub.getDestination(), sub.getConnectionId());
    }

    public void unsubscribe(String subscriptionId) {
        Subscription sub = allSubs.remove(subscriptionId);
        if (sub != null) {
            Set<Subscription> subs = destinationSubs.get(sub.getDestination());
            if (subs != null) {
                subs.remove(sub);
                if (subs.isEmpty()) {
                    destinationSubs.remove(sub.getDestination());
                }
            }
        }
    }

    public void unsubscribeAll(String connectionId) {
        Set<String> toRemove = allSubs.values().stream()
                .filter(s -> s.getConnectionId().equals(connectionId))
                .map(Subscription::getSubscriptionId)
                .collect(Collectors.toSet());
        for (String subId : toRemove) {
            unsubscribe(subId);
        }
    }

    public Set<Subscription> findSubscriptions(String destination) {
        return destinationSubs.getOrDefault(destination, Collections.emptySet());
    }

    public Set<Subscription> findUserSubscriptions(UUID userId, String destination) {
        return findSubscriptions(destination).stream()
                .filter(s -> s.getUserId().equals(userId))
                .collect(Collectors.toSet());
    }
}
