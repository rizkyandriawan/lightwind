package dev.kakrizky.lightwind.search;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for the Lightwind search layer.
 *
 * <p>Properties are read from {@code lightwind.search.*} in application.properties.</p>
 */
@ConfigMapping(prefix = "lightwind.search")
public interface SearchConfig {

    /**
     * Whether the search layer is enabled globally.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Elasticsearch host addresses (host:port).
     */
    @WithDefault("localhost:9200")
    List<String> hosts();

    /**
     * Optional username for Elasticsearch authentication.
     */
    Optional<String> username();

    /**
     * Optional password for Elasticsearch authentication.
     */
    Optional<String> password();

    /**
     * Prefix prepended to all index names.
     */
    @WithDefault("lightwind_")
    String indexPrefix();

    /**
     * Default page size for search queries.
     */
    @WithDefault("20")
    int defaultPageSize();
}
