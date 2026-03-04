package dev.kakrizky.lightwind.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for entity classes that should be indexed in Elasticsearch.
 *
 * <p>When {@link #index()} is empty, the index name is auto-derived from
 * the entity class name (lowercased, e.g. {@code Product} becomes {@code product}).</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Searchable {

    /**
     * Custom Elasticsearch index name. If empty, the lowercase simple class name is used.
     */
    String index() default "";
}
