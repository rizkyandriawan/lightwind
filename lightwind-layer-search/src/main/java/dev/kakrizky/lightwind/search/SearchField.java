package dev.kakrizky.lightwind.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field-level annotation to configure how an entity field is mapped
 * in the Elasticsearch index.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SearchField {

    /**
     * Custom field name in Elasticsearch. If empty, the Java field name is used.
     */
    String name() default "";

    /**
     * Elasticsearch field type.
     */
    SearchFieldType type() default SearchFieldType.TEXT;

    /**
     * Whether this field should be included in full-text search queries.
     */
    boolean searchable() default true;

    /**
     * Boost factor for this field in search relevance scoring.
     */
    float boost() default 1.0f;
}
