package dev.kakrizky.lightwind.realtime.sse;

import dev.kakrizky.lightwind.realtime.RealtimeConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class SseConnectionManager {

    private static final Logger LOG = Logger.getLogger(SseConnectionManager.class);

    @Inject
    RealtimeConfig config;

    private final ConcurrentHashMap<UUID, List<BroadcastProcessor<SseEvent>>> userStreams = new ConcurrentHashMap<>();

    public Multi<SseEvent> createStream(UUID userId) {
        BroadcastProcessor<SseEvent> processor = BroadcastProcessor.create();
        userStreams.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(processor);

        LOG.debugf("SSE stream created for user %s", userId);

        return processor
                .onTermination().invoke(() -> {
                    List<BroadcastProcessor<SseEvent>> processors = userStreams.get(userId);
                    if (processors != null) {
                        processors.remove(processor);
                        if (processors.isEmpty()) {
                            userStreams.remove(userId);
                        }
                    }
                    LOG.debugf("SSE stream closed for user %s", userId);
                });
    }

    public void sendEvent(UUID userId, String eventName, String jsonData) {
        List<BroadcastProcessor<SseEvent>> processors = userStreams.get(userId);
        if (processors != null && !processors.isEmpty()) {
            SseEvent event = new SseEvent(eventName, jsonData);
            for (BroadcastProcessor<SseEvent> processor : processors) {
                try {
                    processor.onNext(event);
                } catch (Exception e) {
                    LOG.warnf("Failed to send SSE event to user %s: %s", userId, e.getMessage());
                }
            }
        }
    }

    public void broadcastEvent(String eventName, String jsonData) {
        SseEvent event = new SseEvent(eventName, jsonData);
        for (var entry : userStreams.entrySet()) {
            for (BroadcastProcessor<SseEvent> processor : entry.getValue()) {
                try {
                    processor.onNext(event);
                } catch (Exception e) {
                    LOG.warnf("Failed to broadcast SSE event to user %s: %s", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    public boolean hasActiveStream(UUID userId) {
        List<BroadcastProcessor<SseEvent>> processors = userStreams.get(userId);
        return processors != null && !processors.isEmpty();
    }
}
