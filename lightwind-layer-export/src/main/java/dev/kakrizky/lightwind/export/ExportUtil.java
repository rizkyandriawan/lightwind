package dev.kakrizky.lightwind.export;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Utility methods for the export layer.
 *
 * <p>Provides reflection-based field value extraction, value formatting,
 * and annotation-driven column discovery.</p>
 */
public final class ExportUtil {

    private ExportUtil() {
    }

    /**
     * Gets the value of a field from an object using its getter method.
     *
     * <p>Looks for a getter following JavaBean conventions: {@code getFieldName()}
     * or {@code isFieldName()} for booleans.</p>
     *
     * @param obj       the object to read from
     * @param fieldName the field name
     * @return the field value, or {@code null} if the getter cannot be invoked
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        try {
            String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method getter = null;

            // Try getXxx() first
            try {
                getter = obj.getClass().getMethod("get" + capitalized);
            } catch (NoSuchMethodException e) {
                // Try isXxx() for booleans
                try {
                    getter = obj.getClass().getMethod("is" + capitalized);
                } catch (NoSuchMethodException e2) {
                    // Ignore
                }
            }

            if (getter != null) {
                return getter.invoke(obj);
            }
        } catch (Exception e) {
            // Fall through
        }

        return null;
    }

    /**
     * Formats a value for display in the export output.
     *
     * @param value      the value to format
     * @param format     a format pattern (e.g., {@code "yyyy-MM-dd"} for dates, {@code "#,##0.00"} for numbers)
     * @param dateFormat the default date format to use when no format pattern is specified
     * @return the formatted string representation
     */
    public static String formatValue(Object value, String format, String dateFormat) {
        if (value == null) {
            return "";
        }

        // Date types
        if (value instanceof LocalDateTime) {
            String pattern = (format != null && !format.isBlank()) ? format : dateFormat;
            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(pattern));
        }
        if (value instanceof LocalDate) {
            String pattern = (format != null && !format.isBlank()) ? format : "yyyy-MM-dd";
            return ((LocalDate) value).format(DateTimeFormatter.ofPattern(pattern));
        }
        if (value instanceof Date) {
            String pattern = (format != null && !format.isBlank()) ? format : dateFormat;
            return new SimpleDateFormat(pattern).format((Date) value);
        }

        // Number types
        if (format != null && !format.isBlank()) {
            if (value instanceof Number) {
                return new DecimalFormat(format).format(value);
            }
        }

        return String.valueOf(value);
    }

    /**
     * Extracts column definitions from {@link ExportColumn} annotations on the given class.
     *
     * <p>Fields annotated with {@code @ExportColumn} are discovered, sorted by
     * their {@link ExportColumn#order()}, and converted into {@link ExportRequest.ColumnDef}
     * instances.</p>
     *
     * @param dtoClass the DTO class to inspect
     * @return a list of column definitions, sorted by order
     */
    public static List<ExportRequest.ColumnDef> extractColumns(Class<?> dtoClass) {
        List<ExportRequest.ColumnDef> columns = new ArrayList<>();

        for (Field field : dtoClass.getDeclaredFields()) {
            ExportColumn annotation = field.getAnnotation(ExportColumn.class);
            if (annotation != null) {
                String header = annotation.header().isBlank() ? field.getName() : annotation.header();
                columns.add(new ExportRequest.ColumnDef(
                        header,
                        field.getName(),
                        annotation.width(),
                        annotation.format()
                ));
            }
        }

        // Sort by the order value stored implicitly — re-read annotations for ordering
        columns.sort(Comparator.comparingInt(col -> {
            try {
                Field field = dtoClass.getDeclaredField(col.getField());
                ExportColumn ann = field.getAnnotation(ExportColumn.class);
                return ann != null ? ann.order() : 0;
            } catch (NoSuchFieldException e) {
                return 0;
            }
        }));

        return columns;
    }
}
