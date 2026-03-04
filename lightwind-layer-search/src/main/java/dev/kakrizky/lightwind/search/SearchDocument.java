package dev.kakrizky.lightwind.search;

import java.util.List;
import java.util.Map;

/**
 * Generic wrapper for a document returned from an Elasticsearch search.
 *
 * @param <T> the source document type
 */
public class SearchDocument<T> {

    private String id;
    private String index;
    private T source;
    private float score;
    private Map<String, List<String>> highlights;

    public SearchDocument() {}

    public SearchDocument(String id, String index, T source, float score, Map<String, List<String>> highlights) {
        this.id = id;
        this.index = index;
        this.source = source;
        this.score = score;
        this.highlights = highlights;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public T getSource() {
        return source;
    }

    public void setSource(T source) {
        this.source = source;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public Map<String, List<String>> getHighlights() {
        return highlights;
    }

    public void setHighlights(Map<String, List<String>> highlights) {
        this.highlights = highlights;
    }
}
