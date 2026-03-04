package dev.kakrizky.lightwind.integration.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kakrizky.lightwind.integration.LightRestClient;
import dev.kakrizky.lightwind.integration.RestRequest;
import dev.kakrizky.lightwind.integration.RestResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing webhook registrations and dispatching events.
 *
 * <p>When an event occurs, this service finds all active webhooks subscribed to
 * that event type, signs the payload with HMAC-SHA256, and delivers it via
 * {@link LightRestClient}. Each delivery attempt is recorded as a
 * {@link WebhookDelivery} for auditing.</p>
 */
@ApplicationScoped
public class WebhookService {

    private static final Logger LOG = Logger.getLogger(WebhookService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Inject
    WebhookConfig config;

    @Inject
    LightRestClient restClient;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Dispatches an event to all active webhooks subscribed to the given event type.
     *
     * @param event the event type (e.g. "order.created")
     * @param data  the event payload
     */
    public void dispatch(String event, Object data) {
        if (!config.enabled()) {
            LOG.debugf("Webhooks disabled, skipping dispatch for event=%s", event);
            return;
        }

        List<WebhookRegistration> webhooks = WebhookRegistration
                .find("active = true")
                .<WebhookRegistration>list();

        for (WebhookRegistration webhook : webhooks) {
            if (webhook.getEventList().contains(event)) {
                try {
                    send(webhook, event, data);
                } catch (Exception e) {
                    LOG.errorf(e, "Failed to dispatch webhook id=%s, event=%s", webhook.getId(), event);
                }
            }
        }
    }

    /**
     * Sends a single webhook delivery, signs the payload, and records the result.
     *
     * @param webhook the target webhook registration
     * @param event   the event type
     * @param data    the event payload
     */
    @Transactional
    public void send(WebhookRegistration webhook, String event, Object data) {
        String deliveryId = UUID.randomUUID().toString();

        try {
            WebhookPayload payload = new WebhookPayload();
            payload.setEvent(event);
            payload.setDeliveryId(deliveryId);
            payload.setTimestamp(LocalDateTime.now());
            payload.setData(data);

            String jsonBody = objectMapper.writeValueAsString(payload);

            // Determine signing secret (per-webhook takes precedence)
            String secret = webhook.getSecret() != null && !webhook.getSecret().isBlank()
                    ? webhook.getSecret()
                    : config.signingSecret();

            String signature = "";
            if (secret != null && !secret.isBlank()) {
                signature = sign(secret, jsonBody);
            }
            payload.setSignature(signature);

            // Re-serialize with signature
            jsonBody = objectMapper.writeValueAsString(payload);

            RestRequest request = RestRequest.builder()
                    .url(webhook.getUrl())
                    .method("POST")
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Event", event)
                    .header("X-Webhook-Delivery", deliveryId)
                    .body(jsonBody);

            if (!signature.isEmpty()) {
                request.header("X-Webhook-Signature", signature);
            }

            RestResponse<String> response = restClient.execute(request);

            // Record delivery
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setWebhookId(webhook.getId());
            delivery.setEvent(event);
            delivery.setPayload(jsonBody);
            delivery.setStatusCode(response.getStatusCode());
            delivery.setResponse(response.getBody());
            delivery.setSuccess(response.isSuccess());
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.persist();

            LOG.debugf("Webhook delivered: webhookId=%s, event=%s, status=%d, success=%s",
                    webhook.getId(), event, response.getStatusCode(), response.isSuccess());

        } catch (Exception e) {
            // Record failed delivery
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setWebhookId(webhook.getId());
            delivery.setEvent(event);
            delivery.setSuccess(false);
            delivery.setError(e.getMessage());
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.persist();

            LOG.errorf(e, "Webhook delivery failed: webhookId=%s, event=%s", webhook.getId(), event);
        }
    }

    /**
     * Lists all registered webhooks.
     *
     * @return list of webhook registrations
     */
    public List<WebhookRegistration> listWebhooks() {
        return WebhookRegistration.listAll();
    }

    /**
     * Registers a new webhook.
     *
     * @param name   a friendly name
     * @param url    the target URL
     * @param events comma-separated event types
     * @param secret optional per-webhook signing secret
     * @return the persisted registration
     */
    @Transactional
    public WebhookRegistration register(String name, String url, String events, String secret) {
        WebhookRegistration webhook = new WebhookRegistration();
        webhook.setName(name);
        webhook.setUrl(url);
        webhook.setEvents(events);
        webhook.setSecret(secret);
        webhook.persist();

        LOG.infof("Webhook registered: name=%s, url=%s, events=%s", name, url, events);
        return webhook;
    }

    /**
     * Unregisters (deletes) a webhook by ID.
     *
     * @param id the webhook ID
     */
    @Transactional
    public void unregister(UUID id) {
        WebhookRegistration webhook = WebhookRegistration.findById(id);
        if (webhook != null) {
            webhook.delete();
            LOG.infof("Webhook unregistered: id=%s", id);
        }
    }

    /**
     * Lists delivery records for a specific webhook with pagination.
     *
     * @param webhookId the webhook ID
     * @param page      page number (0-based)
     * @param size      page size
     * @return list of delivery records
     */
    public List<WebhookDelivery> getDeliveries(UUID webhookId, int page, int size) {
        return WebhookDelivery
                .find("webhookId = ?1 ORDER BY deliveredAt DESC", webhookId)
                .page(page, size)
                .list();
    }

    /**
     * Computes HMAC-SHA256 and returns the hex-encoded signature.
     */
    private String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to compute HMAC-SHA256 signature");
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
