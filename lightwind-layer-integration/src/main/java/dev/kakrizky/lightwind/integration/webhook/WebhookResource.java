package dev.kakrizky.lightwind.integration.webhook;

import dev.kakrizky.lightwind.response.LightResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST resource for managing webhook registrations and viewing deliveries.
 */
@Path("/api/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WebhookResource {

    @Inject
    WebhookService webhookService;

    /**
     * Lists all registered webhooks.
     */
    @GET
    public LightResponse<List<WebhookRegistration>> list() {
        return LightResponse.ok(webhookService.listWebhooks());
    }

    /**
     * Registers a new webhook.
     * Expects JSON body with: name, url, events, secret (optional).
     */
    @POST
    public LightResponse<WebhookRegistration> register(WebhookRegistrationRequest request) {
        WebhookRegistration webhook = webhookService.register(
                request.getName(),
                request.getUrl(),
                request.getEvents(),
                request.getSecret());
        return LightResponse.ok(webhook);
    }

    /**
     * Unregisters a webhook by ID.
     */
    @DELETE
    @Path("/{id}")
    public LightResponse<Void> unregister(@PathParam("id") UUID id) {
        webhookService.unregister(id);
        return LightResponse.ok(null);
    }

    /**
     * Lists delivery records for a specific webhook.
     */
    @GET
    @Path("/{id}/deliveries")
    public LightResponse<List<WebhookDelivery>> deliveries(
            @PathParam("id") UUID id,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return LightResponse.ok(webhookService.getDeliveries(id, page, size));
    }

    /**
     * Sends a test event to a specific webhook.
     */
    @POST
    @Path("/{id}/test")
    public LightResponse<Map<String, String>> test(@PathParam("id") UUID id) {
        WebhookRegistration webhook = WebhookRegistration.findById(id);
        if (webhook == null) {
            return new LightResponse<>(404, null);
        }

        webhookService.send(webhook, "webhook.test", Map.of(
                "message", "This is a test webhook delivery",
                "webhookId", webhook.getId().toString()));

        return LightResponse.ok(Map.of("status", "sent"));
    }

    /**
     * DTO for webhook registration requests.
     */
    public static class WebhookRegistrationRequest {

        private String name;
        private String url;
        private String events;
        private String secret;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getEvents() { return events; }
        public void setEvents(String events) { this.events = events; }

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }
}
