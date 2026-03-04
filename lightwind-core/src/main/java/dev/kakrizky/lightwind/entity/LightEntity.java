package dev.kakrizky.lightwind.entity;

import dev.kakrizky.lightwind.auth.LightUser;
import dev.kakrizky.lightwind.util.BeanUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class LightEntity<E extends LightEntity<E, D>, D>
        extends PanacheEntityBase
        implements SoftDeletable, Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private UUID createdById;

    @Column
    private String createdByName;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private UUID updatedById;

    @Column
    private String updatedByName;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private UUID deletedById;

    @Column
    private String deletedByName;

    @Version
    private Long version;

    // --- Abstract methods ---

    protected abstract Class<D> getDtoClass();
    protected abstract Class<E> getEntityClass();

    // --- Static metadata (override in concrete entity) ---

    public static List<String> getFilterableFields() {
        return Collections.emptyList();
    }

    public static List<String> getSearchableFields() {
        return Collections.emptyList();
    }

    // --- DTO conversion ---

    public D toDto() {
        try {
            D dto = getDtoClass().getDeclaredConstructor().newInstance();
            BeanUtil.copyProperties(this, dto);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert entity to DTO", e);
        }
    }

    public D toCompleteDto() {
        return toDto();
    }

    @SuppressWarnings("unchecked")
    public E fillFromDto(D dto, LightUser user) {
        BeanUtil.copyProperties(dto, this, "id", "createdAt", "updatedAt", "deletedAt", "version");
        if (user != null) {
            if (this.createdById == null) {
                this.createdById = user.userId();
                this.createdByName = user.userName();
            }
            this.updatedById = user.userId();
            this.updatedByName = user.userName();
        }
        return (E) this;
    }

    /**
     * Partial update: only copies non-null fields from DTO (PATCH merge semantics).
     */
    @SuppressWarnings("unchecked")
    public E patchFromDto(D dto, LightUser user) {
        BeanUtil.copyNonNullProperties(dto, this, "id", "createdAt", "updatedAt", "deletedAt", "version");
        if (user != null) {
            this.updatedById = user.userId();
            this.updatedByName = user.userName();
        }
        return (E) this;
    }

    public void markAsDeleted(LightUser user) {
        this.deletedAt = LocalDateTime.now();
        if (user != null) {
            this.deletedById = user.userId();
            this.deletedByName = user.userName();
        }
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedById = null;
        this.deletedByName = null;
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public UUID getCreatedById() { return createdById; }
    public void setCreatedById(UUID createdById) { this.createdById = createdById; }

    @Override
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    @Override
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public UUID getUpdatedById() { return updatedById; }
    public void setUpdatedById(UUID updatedById) { this.updatedById = updatedById; }

    @Override
    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    @Override
    public LocalDateTime getDeletedAt() { return deletedAt; }
    @Override
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public UUID getDeletedById() { return deletedById; }
    public void setDeletedById(UUID deletedById) { this.deletedById = deletedById; }

    public String getDeletedByName() { return deletedByName; }
    public void setDeletedByName(String deletedByName) { this.deletedByName = deletedByName; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
