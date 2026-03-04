package dev.kakrizky.lightwind.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application-scoped service for Elasticsearch indexing and search operations.
 *
 * <p>All index names are automatically prefixed with {@link SearchConfig#indexPrefix()}.</p>
 */
@ApplicationScoped
public class LightSearchService {

    private static final Logger LOG = Logger.getLogger(LightSearchService.class);

    @Inject
    ElasticsearchClient client;

    @Inject
    SearchConfig config;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Indexes a single document.
     *
     * @param indexName the index name (without prefix)
     * @param id        the document ID
     * @param document  the document to index
     */
    public void index(String indexName, String id, Object document) {
        String fullIndex = prefixedIndex(indexName);
        try {
            client.index(i -> i
                    .index(fullIndex)
                    .id(id)
                    .document(document));
            LOG.debugv("Indexed document {0} in {1}", id, fullIndex);
        } catch (Exception e) {
            LOG.errorv("Failed to index document {0} in {1}: {2}", id, fullIndex, e.getMessage());
            throw new RuntimeException("Failed to index document", e);
        }
    }

    /**
     * Bulk indexes multiple documents.
     *
     * @param indexName the index name (without prefix)
     * @param documents map of document ID to document object
     */
    public void bulkIndex(String indexName, Map<String, Object> documents) {
        String fullIndex = prefixedIndex(indexName);
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Map.Entry<String, Object> entry : documents.entrySet()) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(fullIndex)
                                .id(entry.getKey())
                                .document(entry.getValue())));
            }
            BulkResponse response = client.bulk(bulkBuilder.build());
            if (response.errors()) {
                LOG.warnv("Bulk index to {0} completed with errors", fullIndex);
            } else {
                LOG.debugv("Bulk indexed {0} documents in {1}", documents.size(), fullIndex);
            }
        } catch (Exception e) {
            LOG.errorv("Failed to bulk index to {0}: {1}", fullIndex, e.getMessage());
            throw new RuntimeException("Failed to bulk index documents", e);
        }
    }

    /**
     * Deletes a document by ID.
     *
     * @param indexName the index name (without prefix)
     * @param id        the document ID
     */
    public void delete(String indexName, String id) {
        String fullIndex = prefixedIndex(indexName);
        try {
            client.delete(d -> d.index(fullIndex).id(id));
            LOG.debugv("Deleted document {0} from {1}", id, fullIndex);
        } catch (Exception e) {
            LOG.errorv("Failed to delete document {0} from {1}: {2}", id, fullIndex, e.getMessage());
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    /**
     * Searches an index with the given search request.
     *
     * @param indexName the index name (without prefix)
     * @param request   the search request
     * @return search results
     */
    @SuppressWarnings("unchecked")
    public SearchResult<Map<String, Object>> search(String indexName, SearchRequest request) {
        String fullIndex = prefixedIndex(indexName);
        try {
            int from = Math.max(0, request.getPage() - 1) * request.getSize();

            SearchResponse<Map> response = client.search(s -> {
                s.index(fullIndex)
                        .from(from)
                        .size(request.getSize());

                // Build query
                Query query = buildQuery(request);
                if (query != null) {
                    s.query(query);
                }

                // Sorting
                if (request.getSortField() != null && !request.getSortField().isBlank()) {
                    SortOrder order = "desc".equalsIgnoreCase(request.getSortDirection())
                            ? SortOrder.Desc : SortOrder.Asc;
                    s.sort(sort -> sort.field(f -> f.field(request.getSortField()).order(order)));
                }

                // Highlighting
                if (request.isHighlight() && request.getQuery() != null) {
                    s.highlight(h -> {
                        List<String> searchFields = request.getFields().isEmpty()
                                ? List.of("*") : request.getFields();
                        for (String field : searchFields) {
                            h.fields(field, HighlightField.of(hf -> hf));
                        }
                        return h;
                    });
                }

                return s;
            }, Map.class);

            List<SearchDocument<Map<String, Object>>> items = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, List<String>> highlights = new HashMap<>();
                if (hit.highlight() != null) {
                    highlights.putAll(hit.highlight());
                }
                items.add(new SearchDocument<>(
                        hit.id(),
                        hit.index(),
                        (Map<String, Object>) hit.source(),
                        hit.score() != null ? hit.score().floatValue() : 0f,
                        highlights));
            }

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value() : 0L;
            long tookMs = response.took();

            return new SearchResult<>(items, totalHits, request.getPage(), request.getSize(), tookMs);

        } catch (Exception e) {
            LOG.errorv("Failed to search index {0}: {1}", fullIndex, e.getMessage());
            throw new RuntimeException("Failed to execute search", e);
        }
    }

    /**
     * Creates an index with the given mappings.
     *
     * @param indexName the index name (without prefix)
     * @param mappings  the index mappings
     */
    public void createIndex(String indexName, Map<String, Object> mappings) {
        String fullIndex = prefixedIndex(indexName);
        try {
            String mappingsJson = objectMapper.writeValueAsString(mappings);
            InputStream mappingsStream = new ByteArrayInputStream(
                    mappingsJson.getBytes(StandardCharsets.UTF_8));

            client.indices().create(CreateIndexRequest.of(c -> c
                    .index(fullIndex)
                    .withJson(mappingsStream)));
            LOG.infov("Created index {0}", fullIndex);
        } catch (Exception e) {
            LOG.errorv("Failed to create index {0}: {1}", fullIndex, e.getMessage());
            throw new RuntimeException("Failed to create index", e);
        }
    }

    /**
     * Deletes an index.
     *
     * @param indexName the index name (without prefix)
     */
    public void deleteIndex(String indexName) {
        String fullIndex = prefixedIndex(indexName);
        try {
            client.indices().delete(d -> d.index(fullIndex));
            LOG.infov("Deleted index {0}", fullIndex);
        } catch (Exception e) {
            LOG.errorv("Failed to delete index {0}: {1}", fullIndex, e.getMessage());
            throw new RuntimeException("Failed to delete index", e);
        }
    }

    /**
     * Checks whether an index exists.
     *
     * @param indexName the index name (without prefix)
     * @return true if the index exists
     */
    public boolean indexExists(String indexName) {
        String fullIndex = prefixedIndex(indexName);
        try {
            return client.indices().exists(e -> e.index(fullIndex)).value();
        } catch (Exception e) {
            LOG.errorv("Failed to check index existence for {0}: {1}", fullIndex, e.getMessage());
            throw new RuntimeException("Failed to check index existence", e);
        }
    }

    private String prefixedIndex(String indexName) {
        return config.indexPrefix() + indexName;
    }

    private Query buildQuery(SearchRequest request) {
        boolean hasQuery = request.getQuery() != null && !request.getQuery().isBlank();
        boolean hasFilters = request.getFilters() != null && !request.getFilters().isEmpty();

        if (!hasQuery && !hasFilters) {
            return null;
        }

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (hasQuery) {
            if (request.getFields().isEmpty()) {
                boolBuilder.must(m -> m
                        .queryString(qs -> qs.query(request.getQuery())));
            } else {
                boolBuilder.must(m -> m
                        .multiMatch(mm -> mm
                                .query(request.getQuery())
                                .fields(request.getFields())));
            }
        }

        if (hasFilters) {
            for (Map.Entry<String, Object> entry : request.getFilters().entrySet()) {
                boolBuilder.filter(f -> f
                        .term(t -> t
                                .field(entry.getKey())
                                .value(FieldValue.of(entry.getValue().toString()))));
            }
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }
}
