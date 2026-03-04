package dev.kakrizky.lightwind.cache;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the Lightwind caching layer.
 *
 * <p>Properties are read from {@code lightwind.cache.*} in application.properties.</p>
 */
@ConfigMapping(prefix = "lightwind.cache")
public interface LightCacheConfig {

    /**
     * Whether caching is enabled globally.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Default TTL in seconds for cached entries when not specified on the annotation.
     */
    @WithDefault("300")
    long defaultTtl();

    /**
     * Prefix prepended to all cache keys stored in Redis.
     */
    @WithDefault("lightwind:")
    String keyPrefix();
}
