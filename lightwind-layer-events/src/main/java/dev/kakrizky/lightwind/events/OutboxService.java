package dev.kakrizky.lightwind.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing outbox events.
 *
 * <p>Provides methods to persist events to the outbox table and to query/update
 * their status. Uses Jackson for payload serialization.</p>
 */
@ApplicationScoped
public class OutboxService {

    private static final Logger LOG = Logger.getLogger(OutboxService.class);

    @Inject
    ObjectMapper objectMapper;

    /**
     * Saves a new event to the outbox table with PENDING status.
     *
     * @param eventType   the event type identifier
     * @param destination the target topic or queue name
     * @param payload     the event payload object (will be serialized to JSON)
     */
    @Transactional
    public void save(String eventType, String destination, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setDestination(destination);

        try {
            if (payload instanceof String) {
                event.setPayload((String) payload);
            } else {
                event.setPayload(objectMapper.writeValueAsString(payload));
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize outbox event payload for eventType=%s", eventType);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        event.persist();
        LOG.debugf("Saved outbox event: type=%s, destination=%s, id=%s", eventType, destination, event.getId());
    }

    /**
     * Polls for PENDING events ordered by creation time.
     *
     * @param batchSize maximum number of events to return
     * @return list of pending outbox events
     */
    public List<OutboxEvent> pollPending(int batchSize) {
        return OutboxEvent.find("status = ?1 ORDER BY createdAt ASC", OutboxStatus.PENDING)
                .page(0, batchSize)
                .list();
    }

    /**
     * Marks an outbox event as successfully published.
     *
     * @param eventId the ID of the event to mark
     */
    @Transactional
    public void markPublished(UUID eventId) {
        OutboxEvent event = OutboxEvent.findById(eventId);
        if (event != null) {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            LOG.debugf("Marked outbox event as PUBLISHED: id=%s", eventId);
        }
    }

    /**
     * Marks an outbox event as failed and records the error.
     *
     * @param eventId the ID of the event to mark
     * @param error   the error message
     */
    @Transactional
    public void markFailed(UUID eventId, String error) {
        OutboxEvent event = OutboxEvent.findById(eventId);
        if (event != null) {
            event.setStatus(OutboxStatus.FAILED);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(error);
            LOG.warnf("Marked outbox event as FAILED: id=%s, retryCount=%d, error=%s",
                    eventId, event.getRetryCount(), error);
        }
    }
}
