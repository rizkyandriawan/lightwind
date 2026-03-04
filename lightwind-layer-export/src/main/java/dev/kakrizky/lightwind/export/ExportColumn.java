package dev.kakrizky.lightwind.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining export column metadata on DTO fields.
 *
 * <p>When placed on a field, the export engine uses these settings to
 * control column header, ordering, width, and value formatting.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportColumn {

    /**
     * Column header text. Defaults to the field name if empty.
     */
    String header() default "";

    /**
     * Column ordering. Lower values appear first.
     */
    int order() default 0;

    /**
     * Column width in characters. {@code -1} means auto-size.
     */
    int width() default -1;

    /**
     * Format pattern for dates or numbers. Empty means no special formatting.
     */
    String format() default "";
}
