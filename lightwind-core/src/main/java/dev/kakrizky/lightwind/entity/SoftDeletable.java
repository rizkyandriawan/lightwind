package dev.kakrizky.lightwind.entity;

import java.time.LocalDateTime;

public interface SoftDeletable {

    LocalDateTime getDeletedAt();
    void setDeletedAt(LocalDateTime deletedAt);

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
