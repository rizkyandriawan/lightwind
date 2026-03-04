package dev.kakrizky.lightwind.crud;

import dev.kakrizky.lightwind.auth.LightUser;
import dev.kakrizky.lightwind.entity.LightEntity;
import dev.kakrizky.lightwind.exception.BadRequestException;
import dev.kakrizky.lightwind.exception.ObjectNotFoundException;
import dev.kakrizky.lightwind.exception.ValidationError;
import dev.kakrizky.lightwind.exception.ValidationErrorException;
import dev.kakrizky.lightwind.query.LightQueryEngine;
import dev.kakrizky.lightwind.response.PagedResult;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class LightCrudService<E extends LightEntity<E, D>, D> {

    @Inject
    EntityManager em;

    protected abstract Class<E> getEntityClass();
    protected abstract Class<D> getDtoClass();

    // --- Validation hooks ---

    protected List<ValidationError> validateCreate(D dto) { return List.of(); }
    protected List<ValidationError> validateUpdate(D dto) { return List.of(); }

    // --- Lifecycle hooks ---

    protected void beforeCreate(D dto, E entity) {}
    protected void afterCreate(E entity) {}
    protected void beforeUpdate(D dto, E entity) {}
    protected void afterUpdate(E entity) {}
    protected void beforeRemove(E entity) {}

    // --- CRUD operations ---

    @Transactional
    public PagedResult<D> getAll(Map<String, List<String>> filters, String search,
                                  int page, int size, String sort, String sortDir,
                                  LightUser user) {
        return new LightQueryEngine<>(em, getEntityClass())
                .applyFilters(filters, getAllowedFilterFields())
                .applySearch(search, getSearchableFields())
                .applySoftDeleteScope()
                .execute(page, size, sort, sortDir, getDtoClass());
    }

    @Transactional
    public PagedResult<D> getAllIncludingDeleted(Map<String, List<String>> filters,
                                                  String search, int page, int size,
                                                  String sort, String sortDir) {
        return new LightQueryEngine<>(em, getEntityClass())
                .applyFilters(filters, getAllowedFilterFields())
                .applySearch(search, getSearchableFields())
                .execute(page, size, sort, sortDir, getDtoClass());
    }

    public D getOne(UUID id, LightUser user) {
        E entity = em.find(getEntityClass(), id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new ObjectNotFoundException(getEntityClass().getSimpleName() + " not found");
        }
        return entity.toCompleteDto();
    }

    @Transactional
    public D create(D dto, LightUser user) {
        List<ValidationError> errors = validateCreate(dto);
        if (!errors.isEmpty()) {
            throw new ValidationErrorException("Validation failed", errors);
        }

        try {
            E entity = getEntityClass().getDeclaredConstructor().newInstance();
            entity.fillFromDto(dto, user);
            beforeCreate(dto, entity);
            entity.persist();
            afterCreate(entity);
            return entity.toCompleteDto();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Entity creation failed", e);
        }
    }

    @Transactional
    public D update(UUID id, D dto, LightUser user) {
        E entity = em.find(getEntityClass(), id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new ObjectNotFoundException(getEntityClass().getSimpleName() + " not found");
        }

        List<ValidationError> errors = validateUpdate(dto);
        if (!errors.isEmpty()) {
            throw new ValidationErrorException("Validation failed", errors);
        }

        entity.fillFromDto(dto, user);
        beforeUpdate(dto, entity);
        entity.persist();
        afterUpdate(entity);
        return entity.toCompleteDto();
    }

    @Transactional
    public D delete(UUID id, LightUser user) {
        E entity = em.find(getEntityClass(), id);
        if (entity == null) {
            throw new ObjectNotFoundException(getEntityClass().getSimpleName() + " not found");
        }
        if (entity.getDeletedAt() != null) {
            throw new ObjectNotFoundException(getEntityClass().getSimpleName() + " already deleted");
        }

        beforeRemove(entity);
        entity.markAsDeleted(user);
        entity.persist();
        return entity.toDto();
    }

    @Transactional
    public D restore(UUID id, LightUser user) {
        E entity = em.find(getEntityClass(), id);
        if (entity == null) {
            throw new ObjectNotFoundException(getEntityClass().getSimpleName() + " not found");
        }
        if (entity.getDeletedAt() == null) {
            throw new BadRequestException(getEntityClass().getSimpleName() + " is not deleted");
        }

        entity.restore();
        entity.persist();
        return entity.toCompleteDto();
    }

    // --- Metadata helpers ---

    @SuppressWarnings("unchecked")
    protected List<String> getAllowedFilterFields() {
        try {
            Method method = getEntityClass().getMethod("getFilterableFields");
            return (List<String>) method.invoke(null);
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> getSearchableFields() {
        try {
            Method method = getEntityClass().getMethod("getSearchableFields");
            return (List<String>) method.invoke(null);
        } catch (Exception e) {
            return List.of();
        }
    }
}
