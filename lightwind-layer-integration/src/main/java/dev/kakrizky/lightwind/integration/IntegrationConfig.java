package dev.kakrizky.lightwind.integration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the Lightwind integration layer.
 *
 * <p>Properties are read from {@code lightwind.integration.*} in application.properties.</p>
 */
@ConfigMapping(prefix = "lightwind.integration")
public interface IntegrationConfig {

    /**
     * Default timeout in seconds for HTTP requests.
     */
    @WithDefault("30")
    int defaultTimeout();

    /**
     * Maximum number of retries for failed requests.
     */
    @WithDefault("3")
    int maxRetries();

    /**
     * Whether the circuit breaker is enabled globally.
     */
    @WithDefault("true")
    boolean circuitBreakerEnabled();
}
