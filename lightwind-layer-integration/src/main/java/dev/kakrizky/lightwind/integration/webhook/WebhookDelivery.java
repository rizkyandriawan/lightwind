package dev.kakrizky.lightwind.integration.webhook;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity recording the result of a webhook delivery attempt.
 *
 * <p>Each delivery tracks the HTTP status, response body, retry count, and whether
 * the delivery was successful. Used for auditing and retry logic.</p>
 */
@Entity
@Table(name = "lightwind_webhook_deliveries", indexes = {
        @Index(name = "idx_webhook_delivery_webhook_id", columnList = "webhookId")
})
public class WebhookDelivery extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID webhookId;

    @Column(nullable = false)
    private String event;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column
    private int statusCode;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column
    private int retryCount;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private LocalDateTime deliveredAt;

    @Column
    private String error;

    public WebhookDelivery() {
        this.deliveredAt = LocalDateTime.now();
        this.retryCount = 0;
        this.success = false;
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getWebhookId() { return webhookId; }
    public void setWebhookId(UUID webhookId) { this.webhookId = webhookId; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
