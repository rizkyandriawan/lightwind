package dev.kakrizky.lightwind.search;

import java.util.Map;

/**
 * Interface for mapping entities to Elasticsearch documents.
 *
 * <p>Implementations define how an entity is converted to a search document,
 * what the index name is, and what mappings the index should have.</p>
 */
public interface SearchIndexer {

    /**
     * Returns the Elasticsearch index name for this entity type.
     * The {@link SearchConfig#indexPrefix()} will be prepended automatically.
     */
    String getIndexName();

    /**
     * Converts an entity to a flat map suitable for Elasticsearch indexing.
     *
     * @param entity the entity to convert
     * @return a map of field names to values
     */
    Map<String, Object> toDocument(Object entity);

    /**
     * Returns the Elasticsearch index mappings for this entity type.
     *
     * <p>The returned map should follow the Elasticsearch mappings format,
     * e.g. {@code {"properties": {"title": {"type": "text"}, "status": {"type": "keyword"}}}}.</p>
     *
     * @return the index mappings
     */
    Map<String, Object> getIndexMappings();
}
