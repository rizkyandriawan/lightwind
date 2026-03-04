package dev.kakrizky.lightwind.search;

import java.util.List;

/**
 * Generic result container for Elasticsearch search responses.
 *
 * @param <T> the source document type
 */
public class SearchResult<T> {

    private List<SearchDocument<T>> items;
    private long totalHits;
    private int page;
    private int size;
    private long tookMs;

    public SearchResult() {}

    public SearchResult(List<SearchDocument<T>> items, long totalHits, int page, int size, long tookMs) {
        this.items = items;
        this.totalHits = totalHits;
        this.page = page;
        this.size = size;
        this.tookMs = tookMs;
    }

    public List<SearchDocument<T>> getItems() {
        return items;
    }

    public void setItems(List<SearchDocument<T>> items) {
        this.items = items;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTookMs() {
        return tookMs;
    }

    public void setTookMs(long tookMs) {
        this.tookMs = tookMs;
    }
}
