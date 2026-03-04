package dev.kakrizky.lightwind.integration.webhook;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the webhook subsystem.
 *
 * <p>Properties are read from {@code lightwind.webhook.*} in application.properties.</p>
 */
@ConfigMapping(prefix = "lightwind.webhook")
public interface WebhookConfig {

    /**
     * Whether webhooks are enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Maximum number of delivery retries per webhook dispatch.
     */
    @WithDefault("5")
    int maxRetries();

    /**
     * Global HMAC-SHA256 signing secret. Per-webhook secrets take precedence.
     */
    @WithDefault("")
    String signingSecret();
}
