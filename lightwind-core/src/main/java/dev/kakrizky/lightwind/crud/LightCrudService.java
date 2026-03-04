package dev.kakrizky.lightwind.crud;

import dev.kakrizky.lightwind.audit.AuditAction;
import dev.kakrizky.lightwind.audit.AuditLogService;
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

    @Inject
    AuditLogService auditLogService;

    protected abstract Class<E> getEntityClass();
    protected abstract Class<D> getDtoClass();

    /**
     * Override to enable audit logging for this service.
     */
    protected boolean isAuditEnabled() { return false; }

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
            D result = entity.toCompleteDto();
            if (isAuditEnabled()) {
                auditLogService.log(AuditAction.CREATE, entity, null, result, user);
            }
            return result;
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

        D previousDto = isAuditEnabled() ? entity.toCompleteDto() : null;

        List<ValidationError> errors = validateUpdate(dto);
        if (!errors.isEmpty()) {
            throw new ValidationErrorException("Validation failed", errors);
        }

        entity.fillFromDto(dto, user);
        beforeUpdate(dto, entity);
        entity.persist();
        afterUpdate(entity);
        D result = entity.toCompleteDto();
        if (isAuditEnabled()) {
            auditLogService.log(AuditAction.UPDATE, entity, previousDto, result, user);
        }
        return result;
    }

    @Transactional
    public D patch(UUID id, D dto, LightUser user) {
        E entity = em.find(getEntityClass(), id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new ObjectNotFoundException(getEntityClass().getSimpleName() + " not found");
        }

        D previousDto = isAuditEnabled() ? entity.toCompleteDto() : null;

        entity.patchFromDto(dto, user);
        beforeUpdate(dto, entity);
        entity.persist();
        afterUpdate(entity);
        D result = entity.toCompleteDto();
        if (isAuditEnabled()) {
            auditLogService.log(AuditAction.UPDATE, entity, previousDto, result, user);
        }
        return result;
    }

    @Transactional
    public List<D> bulkCreate(List<D> dtos, LightUser user) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("Request body must contain at least one item");
        }

        List<D> results = new java.util.ArrayList<>();
        for (D dto : dtos) {
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
                results.add(entity.toCompleteDto());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Entity creation failed", e);
            }
        }
        return results;
    }

    @Transactional
    public int bulkDelete(List<UUID> ids, LightUser user) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Request body must contain at least one ID");
        }

        int count = 0;
        for (UUID id : ids) {
            E entity = em.find(getEntityClass(), id);
            if (entity != null && entity.getDeletedAt() == null) {
                beforeRemove(entity);
                entity.markAsDeleted(user);
                entity.persist();
                count++;
            }
        }
        return count;
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
        D result = entity.toDto();
        if (isAuditEnabled()) {
            auditLogService.log(AuditAction.DELETE, entity, result, null, user);
        }
        return result;
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
        D result = entity.toCompleteDto();
        if (isAuditEnabled()) {
            auditLogService.log(AuditAction.RESTORE, entity, null, result, user);
        }
        return result;
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
