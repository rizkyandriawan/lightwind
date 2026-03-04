package dev.kakrizky.lightwind.query;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FilterOperatorTest {

    @Test
    void fromFieldName_returnsEquals_forPlainField() {
        assertEquals(FilterOperator.EQUALS, FilterOperator.fromFieldName("name"));
    }

    @Test
    void fromFieldName_parsesGreaterThan() {
        assertEquals(FilterOperator.GREATER_THAN, FilterOperator.fromFieldName("price__gt"));
    }

    @Test
    void fromFieldName_parsesGreaterThanOrEqual() {
        assertEquals(FilterOperator.GREATER_THAN_OR_EQUAL, FilterOperator.fromFieldName("price__gte"));
    }

    @Test
    void fromFieldName_parsesLessThan() {
        assertEquals(FilterOperator.LESS_THAN, FilterOperator.fromFieldName("price__lt"));
    }

    @Test
    void fromFieldName_parsesLessThanOrEqual() {
        assertEquals(FilterOperator.LESS_THAN_OR_EQUAL, FilterOperator.fromFieldName("price__lte"));
    }

    @Test
    void fromFieldName_parsesNotEqual() {
        assertEquals(FilterOperator.NOT_EQUAL, FilterOperator.fromFieldName("status__ne"));
    }

    @Test
    void fromFieldName_parsesLike() {
        assertEquals(FilterOperator.LIKE, FilterOperator.fromFieldName("name__like"));
    }

    @Test
    void fromFieldName_parsesContains() {
        assertEquals(FilterOperator.CONTAINS, FilterOperator.fromFieldName("name__contains"));
    }

    @Test
    void fromFieldName_parsesStartsWith() {
        assertEquals(FilterOperator.STARTS_WITH, FilterOperator.fromFieldName("name__startswith"));
    }

    @Test
    void fromFieldName_parsesEndsWith() {
        assertEquals(FilterOperator.ENDS_WITH, FilterOperator.fromFieldName("name__endswith"));
    }

    @Test
    void fromFieldName_parsesIn() {
        assertEquals(FilterOperator.IN, FilterOperator.fromFieldName("status__in"));
    }

    @Test
    void fromFieldName_parsesBetween() {
        assertEquals(FilterOperator.BETWEEN, FilterOperator.fromFieldName("price__between"));
    }

    @Test
    void fromFieldName_parsesIsNull() {
        assertEquals(FilterOperator.IS_NULL, FilterOperator.fromFieldName("category__isnull"));
    }

    @Test
    void extractFieldName_returnsFieldWithoutSuffix() {
        assertEquals("price", FilterOperator.extractFieldName("price__gte"));
        assertEquals("name", FilterOperator.extractFieldName("name__like"));
        assertEquals("category", FilterOperator.extractFieldName("category__isnull"));
    }

    @Test
    void extractFieldName_returnsFieldAsIs_forPlainField() {
        assertEquals("name", FilterOperator.extractFieldName("name"));
        assertEquals("price", FilterOperator.extractFieldName("price"));
    }
}
