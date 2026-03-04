package dev.kakrizky.lightwind.realtime.connection;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ConnectionManager {

    private static final Logger LOG = Logger.getLogger(ConnectionManager.class);

    private final ConcurrentHashMap<UUID, Set<String>> userConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RealtimeConnection> connections = new ConcurrentHashMap<>();

    public void addConnection(String connectionId, RealtimeConnection connection) {
        connections.put(connectionId, connection);
        userConnections.computeIfAbsent(connection.getUserId(), k -> ConcurrentHashMap.newKeySet())
                .add(connectionId);
        LOG.debugf("Connection added: %s for user %s (total: %d)", connectionId, connection.getUserId(), connections.size());
    }

    public void removeConnection(String connectionId) {
        RealtimeConnection conn = connections.remove(connectionId);
        if (conn != null) {
            Set<String> connIds = userConnections.get(conn.getUserId());
            if (connIds != null) {
                connIds.remove(connectionId);
                if (connIds.isEmpty()) {
                    userConnections.remove(conn.getUserId());
                }
            }
            LOG.debugf("Connection removed: %s for user %s", connectionId, conn.getUserId());
        }
    }

    public Set<RealtimeConnection> getConnectionsForUser(UUID userId) {
        Set<String> connIds = userConnections.getOrDefault(userId, Collections.emptySet());
        Set<RealtimeConnection> result = ConcurrentHashMap.newKeySet();
        for (String connId : connIds) {
            RealtimeConnection conn = connections.get(connId);
            if (conn != null) {
                result.add(conn);
            }
        }
        return result;
    }

    public Optional<RealtimeConnection> getConnection(String connectionId) {
        return Optional.ofNullable(connections.get(connectionId));
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public int getUserCount() {
        return userConnections.size();
    }
}
