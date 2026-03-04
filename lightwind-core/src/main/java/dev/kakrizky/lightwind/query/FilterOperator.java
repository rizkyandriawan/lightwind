package dev.kakrizky.lightwind.query;

public enum FilterOperator {
    EQUALS(""),
    GREATER_THAN("__gt"),
    GREATER_THAN_OR_EQUAL("__gte"),
    LESS_THAN("__lt"),
    LESS_THAN_OR_EQUAL("__lte"),
    NOT_EQUAL("__ne"),
    LIKE("__like"),
    CONTAINS("__contains"),
    STARTS_WITH("__startswith"),
    ENDS_WITH("__endswith"),
    IN("__in"),
    BETWEEN("__between"),
    IS_NULL("__isnull");

    private final String suffix;

    FilterOperator(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public static FilterOperator fromFieldName(String fieldName) {
        for (FilterOperator op : values()) {
            if (op != EQUALS && fieldName.endsWith(op.suffix)) {
                return op;
            }
        }
        return EQUALS;
    }

    public static String extractFieldName(String fieldName) {
        for (FilterOperator op : values()) {
            if (op != EQUALS && fieldName.endsWith(op.suffix)) {
                return fieldName.substring(0, fieldName.length() - op.suffix.length());
            }
        }
        return fieldName;
    }
}
