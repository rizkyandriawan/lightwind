package dev.kakrizky.lightwind.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base event class for the Lightwind event system.
 *
 * <p>Carries event metadata including the type, JSON payload, source entity
 * information, and a correlation ID for tracking event chains.</p>
 */
public class LightEvent {

    private String eventType;
    private String payload;
    private UUID sourceId;
    private String sourceType;
    private LocalDateTime timestamp;
    private UUID correlationId;

    public LightEvent() {
        this.timestamp = LocalDateTime.now();
        this.correlationId = UUID.randomUUID();
    }

    public LightEvent(String eventType, String payload, UUID sourceId, String sourceType) {
        this();
        this.eventType = eventType;
        this.payload = payload;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
    }

    // --- Getters and Setters ---

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
}
