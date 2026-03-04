package dev.kakrizky.lightwind.search;

/**
 * Supported Elasticsearch field types for {@link SearchField} annotations.
 */
public enum SearchFieldType {
    TEXT,
    KEYWORD,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    DATE,
    NESTED,
    OBJECT
}
