package dev.kakrizky.lightwind.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Helper service for publishing entity lifecycle events.
 *
 * <p>Provides convenience methods for common CRUD events (created, updated, deleted).
 * When {@code lightwind.events.outbox.enabled} is {@code true}, events are also
 * persisted to the outbox table for reliable delivery.</p>
 */
@ApplicationScoped
public class EntityEventPublisher {

    private static final Logger LOG = Logger.getLogger(EntityEventPublisher.class);

    @Inject
    EventBus eventBus;

    @Inject
    OutboxService outboxService;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "lightwind.events.outbox.enabled", defaultValue = "false")
    boolean outboxEnabled;

    /**
     * Publishes an entity-created event.
     *
     * @param entityType the type name of the entity
     * @param entityId   the ID of the created entity
     * @param dto        the DTO representation of the created entity
     */
    public void entityCreated(String entityType, UUID entityId, Object dto) {
        String eventType = entityType + ".CREATED";
        String payload = serialize(dto);

        LightEvent event = new LightEvent(eventType, payload, entityId, entityType);
        eventBus.publish(event);

        if (outboxEnabled) {
            outboxService.save(eventType, entityType, payload);
        }

        LOG.debugf("Published entity created event: type=%s, id=%s", entityType, entityId);
    }

    /**
     * Publishes an entity-updated event with both previous and new state.
     *
     * @param entityType  the type name of the entity
     * @param entityId    the ID of the updated entity
     * @param previousDto the DTO representation before the update
     * @param newDto      the DTO representation after the update
     */
    public void entityUpdated(String entityType, UUID entityId, Object previousDto, Object newDto) {
        String eventType = entityType + ".UPDATED";
        String payload = serializeUpdatePayload(previousDto, newDto);

        LightEvent event = new LightEvent(eventType, payload, entityId, entityType);
        eventBus.publish(event);

        if (outboxEnabled) {
            outboxService.save(eventType, entityType, payload);
        }

        LOG.debugf("Published entity updated event: type=%s, id=%s", entityType, entityId);
    }

    /**
     * Publishes an entity-deleted event.
     *
     * @param entityType the type name of the entity
     * @param entityId   the ID of the deleted entity
     * @param dto        the DTO representation of the deleted entity
     */
    public void entityDeleted(String entityType, UUID entityId, Object dto) {
        String eventType = entityType + ".DELETED";
        String payload = serialize(dto);

        LightEvent event = new LightEvent(eventType, payload, entityId, entityType);
        eventBus.publish(event);

        if (outboxEnabled) {
            outboxService.save(eventType, entityType, payload);
        }

        LOG.debugf("Published entity deleted event: type=%s, id=%s", entityType, entityId);
    }

    private String serialize(Object obj) {
        try {
            if (obj instanceof String) {
                return (String) obj;
            }
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize event payload");
            return "{}";
        }
    }

    private String serializeUpdatePayload(Object previousDto, Object newDto) {
        try {
            String previous = serialize(previousDto);
            String current = serialize(newDto);
            return objectMapper.writeValueAsString(
                    java.util.Map.of("previous", objectMapper.readTree(previous),
                                     "current", objectMapper.readTree(current)));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize update event payload");
            return "{}";
        }
    }
}
