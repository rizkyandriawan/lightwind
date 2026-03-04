package dev.kakrizky.lightwind.query;

import dev.kakrizky.lightwind.entity.LightEntity;
import dev.kakrizky.lightwind.entity.SoftDeletable;
import dev.kakrizky.lightwind.response.PageMeta;
import dev.kakrizky.lightwind.response.PagedResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LightQueryEngine<E> {

    private final EntityManager em;
    private final Class<E> entityClass;

    private Map<String, List<String>> filters;
    private List<String> allowedFields = List.of();
    private String searchKeyword;
    private List<String> searchableFields = List.of();
    private boolean applySoftDelete = false;

    public LightQueryEngine(EntityManager em, Class<E> entityClass) {
        this.em = em;
        this.entityClass = entityClass;
    }

    public LightQueryEngine<E> applyFilters(Map<String, List<String>> filters, List<String> allowedFields) {
        this.filters = filters;
        this.allowedFields = allowedFields != null ? allowedFields : List.of();
        return this;
    }

    public LightQueryEngine<E> applySearch(String keyword, List<String> searchableFields) {
        this.searchKeyword = keyword;
        this.searchableFields = searchableFields != null ? searchableFields : List.of();
        return this;
    }

    public LightQueryEngine<E> applySoftDeleteScope() {
        if (SoftDeletable.class.isAssignableFrom(entityClass)) {
            this.applySoftDelete = true;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <D> PagedResult<D> execute(int page, int size, String sortField, String sortDir, Class<D> dtoClass) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<E> countRoot = countQuery.from(entityClass);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot);
        countQuery.select(cb.count(countRoot));
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(new Predicate[0]));
        }
        long total = em.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<E> dataQuery = cb.createQuery(entityClass);
        Root<E> dataRoot = dataQuery.from(entityClass);
        List<Predicate> dataPredicates = buildPredicates(cb, dataRoot);
        dataQuery.select(dataRoot);
        if (!dataPredicates.isEmpty()) {
            dataQuery.where(dataPredicates.toArray(new Predicate[0]));
        }

        // Sort
        if (sortField != null && !sortField.isBlank()) {
            Order order = "desc".equalsIgnoreCase(sortDir)
                    ? cb.desc(dataRoot.get(sortField))
                    : cb.asc(dataRoot.get(sortField));
            dataQuery.orderBy(order);
        } else {
            try {
                dataQuery.orderBy(cb.desc(dataRoot.get("createdAt")));
            } catch (IllegalArgumentException ignored) {
                // Entity doesn't have createdAt, skip default sort
            }
        }

        List<E> entities = em.createQuery(dataQuery)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size)
                .getResultList();

        // Map to DTOs
        List<D> content = entities.stream()
                .map(e -> ((LightEntity<?, D>) e).toDto())
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PagedResult<>(content, new PageMeta(page, size, total, totalPages));
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<E> root) {
        List<Predicate> predicates = new ArrayList<>();

        // Soft delete scope
        if (applySoftDelete) {
            predicates.add(cb.isNull(root.get("deletedAt")));
        }

        // Filters
        if (filters != null) {
            for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
                FilterOperator op = FilterOperator.fromFieldName(entry.getKey());
                String field = FilterOperator.extractFieldName(entry.getKey());

                if (!allowedFields.contains(field)) continue;

                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) continue;

                try {
                    Path<?> path = getPath(root, field);
                    Class<?> javaType = path.getJavaType();
                    Predicate predicate = buildPredicate(cb, path, op, values, javaType);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Skip invalid fields
                }
            }
        }

        // Search
        if (searchKeyword != null && !searchKeyword.isBlank() && !searchableFields.isEmpty()) {
            String pattern = "%" + searchKeyword.toLowerCase() + "%";
            Predicate[] orPredicates = searchableFields.stream()
                    .map(f -> cb.like(cb.lower(root.get(f)), pattern))
                    .toArray(Predicate[]::new);
            predicates.add(cb.or(orPredicates));
        }

        return predicates;
    }

    @SuppressWarnings("unchecked")
    private Predicate buildPredicate(CriteriaBuilder cb, Path<?> path,
                                      FilterOperator op, List<String> values, Class<?> javaType) {
        String value = values.get(0);

        return switch (op) {
            case EQUALS -> {
                if (values.size() > 1) {
                    List<Object> converted = values.stream()
                            .map(v -> convertValue(v, javaType)).toList();
                    yield ((Path<Object>) path).in(converted);
                }
                yield cb.equal(path, convertValue(value, javaType));
            }
            case NOT_EQUAL -> cb.notEqual(path, convertValue(value, javaType));
            case GREATER_THAN -> cb.greaterThan((Path<Comparable>) path, (Comparable) convertValue(value, javaType));
            case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) convertValue(value, javaType));
            case LESS_THAN -> cb.lessThan((Path<Comparable>) path, (Comparable) convertValue(value, javaType));
            case LESS_THAN_OR_EQUAL -> cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) convertValue(value, javaType));
            case LIKE, CONTAINS -> cb.like(cb.lower((Path<String>) path), "%" + value.toLowerCase() + "%");
            case STARTS_WITH -> cb.like(cb.lower((Path<String>) path), value.toLowerCase() + "%");
            case ENDS_WITH -> cb.like(cb.lower((Path<String>) path), "%" + value.toLowerCase());
            case IN -> {
                List<Object> inValues = values.stream()
                        .map(v -> convertValue(v, javaType)).toList();
                yield ((Path<Object>) path).in(inValues);
            }
            case BETWEEN -> {
                if (values.size() >= 2) {
                    Comparable min = (Comparable) convertValue(values.get(0), javaType);
                    Comparable max = (Comparable) convertValue(values.get(1), javaType);
                    yield cb.between((Path<Comparable>) path, min, max);
                }
                yield null;
            }
            case IS_NULL -> "true".equalsIgnoreCase(value) ? cb.isNull(path) : cb.isNotNull(path);
        };
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;

        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(value);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(value);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value);
            if (targetType == UUID.class) return UUID.fromString(value);
            if (targetType == LocalDateTime.class) return parseDateTime(value);
            if (targetType == LocalDate.class) return LocalDate.parse(value);
            if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, value);
            return value;
        } catch (Exception e) {
            return value;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException e2) {
                return LocalDate.parse(value).atStartOfDay();
            }
        }
    }

    private Path<?> getPath(Root<E> root, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }
}
