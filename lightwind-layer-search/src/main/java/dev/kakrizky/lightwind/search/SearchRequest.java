package dev.kakrizky.lightwind.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder-pattern class for constructing Elasticsearch search requests.
 *
 * <p>Use {@link #builder()} to create a new instance, then chain
 * configuration methods before passing to {@link LightSearchService#search}.</p>
 */
public class SearchRequest {

    private String query;
    private Map<String, Object> filters;
    private List<String> fields;
    private int page;
    private int size;
    private String sortField;
    private String sortDirection;
    private boolean highlight;

    private SearchRequest() {
        this.filters = new HashMap<>();
        this.fields = new ArrayList<>();
        this.page = 1;
        this.size = 20;
        this.sortDirection = "asc";
        this.highlight = false;
    }

    /**
     * Creates a new SearchRequest builder.
     */
    public static SearchRequest builder() {
        return new SearchRequest();
    }

    public SearchRequest query(String query) {
        this.query = query;
        return this;
    }

    public SearchRequest filters(Map<String, Object> filters) {
        this.filters = filters;
        return this;
    }

    public SearchRequest filter(String field, Object value) {
        this.filters.put(field, value);
        return this;
    }

    public SearchRequest fields(List<String> fields) {
        this.fields = fields;
        return this;
    }

    public SearchRequest field(String field) {
        this.fields.add(field);
        return this;
    }

    public SearchRequest page(int page) {
        this.page = page;
        return this;
    }

    public SearchRequest size(int size) {
        this.size = size;
        return this;
    }

    public SearchRequest sortField(String sortField) {
        this.sortField = sortField;
        return this;
    }

    public SearchRequest sortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
        return this;
    }

    public SearchRequest highlight(boolean highlight) {
        this.highlight = highlight;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public List<String> getFields() {
        return fields;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSortField() {
        return sortField;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public boolean isHighlight() {
        return highlight;
    }
}
