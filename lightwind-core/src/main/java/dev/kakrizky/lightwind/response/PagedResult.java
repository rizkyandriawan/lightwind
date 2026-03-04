package dev.kakrizky.lightwind.response;

import java.util.List;

public class PagedResult<T> {

    private List<T> items;
    private PageMeta meta;

    public PagedResult() {}

    public PagedResult(List<T> items, PageMeta meta) {
        this.items = items;
        this.meta = meta;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public PageMeta getMeta() { return meta; }
    public void setMeta(PageMeta meta) { this.meta = meta; }
}
