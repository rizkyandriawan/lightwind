package dev.kakrizky.lightwind.search;

import dev.kakrizky.lightwind.response.LightResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Abstract REST resource for Elasticsearch search endpoints.
 *
 * <p>Subclasses must define a {@code @Path} and implement {@link #reindex()}
 * to provide data for reindexing.</p>
 *
 * <p>Provides:</p>
 * <ul>
 *     <li>{@code GET /} — search with query params ({@code q}, {@code page}, {@code size}, {@code sort}, {@code fields})</li>
 *     <li>{@code POST /_reindex} — trigger a full reindex</li>
 * </ul>
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class SearchResource {

    @Inject
    LightSearchService searchService;

    /**
     * Returns the index name (without prefix) for this resource.
     */
    protected abstract String getIndexName();

    /**
     * Performs a full reindex. Subclasses should load all entities,
     * convert them to documents, and index them via {@link LightSearchService}.
     *
     * @return the number of documents indexed
     */
    protected abstract int reindex();

    /**
     * Search endpoint with query parameters.
     */
    @GET
    public LightResponse<SearchResult<Map<String, Object>>> search(
            @QueryParam("q") String query,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") String sort,
            @QueryParam("fields") String fields) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .page(page)
                .size(size)
                .highlight(query != null && !query.isBlank());

        if (sort != null && !sort.isBlank()) {
            String[] sortParts = sort.split(":");
            request.sortField(sortParts[0]);
            if (sortParts.length > 1) {
                request.sortDirection(sortParts[1]);
            }
        }

        if (fields != null && !fields.isBlank()) {
            List<String> fieldList = Arrays.asList(fields.split(","));
            request.fields(fieldList);
        }

        SearchResult<Map<String, Object>> result = searchService.search(
                getIndexName(), request);
        return LightResponse.ok(result);
    }

    /**
     * Trigger a full reindex of all documents.
     */
    @POST
    @Path("/_reindex")
    public LightResponse<ReindexResponse> triggerReindex() {
        int count = reindex();
        return LightResponse.ok(new ReindexResponse(count));
    }

    /**
     * Response DTO for reindex operations.
     */
    public static class ReindexResponse {
        private int documentsIndexed;

        public ReindexResponse() {}

        public ReindexResponse(int documentsIndexed) {
            this.documentsIndexed = documentsIndexed;
        }

        public int getDocumentsIndexed() {
            return documentsIndexed;
        }

        public void setDocumentsIndexed(int documentsIndexed) {
            this.documentsIndexed = documentsIndexed;
        }
    }
}
