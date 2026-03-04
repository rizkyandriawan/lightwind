package dev.kakrizky.lightwind.integration.webhook;

import java.time.LocalDateTime;

/**
 * DTO representing the payload sent to a webhook endpoint.
 *
 * <p>Contains the event type, a unique delivery ID for idempotency,
 * the event data, and an HMAC-SHA256 signature.</p>
 */
public class WebhookPayload {

    private String event;
    private String deliveryId;
    private LocalDateTime timestamp;
    private Object data;
    private String signature;

    public WebhookPayload() {
    }

    public WebhookPayload(String event, String deliveryId, LocalDateTime timestamp, Object data, String signature) {
        this.event = event;
        this.deliveryId = deliveryId;
        this.timestamp = timestamp;
        this.data = data;
        this.signature = signature;
    }

    // --- Getters and Setters ---

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
