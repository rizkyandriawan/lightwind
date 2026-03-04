package dev.kakrizky.lightwind.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public interface Auditable {

    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    UUID getCreatedById();
    String getCreatedByName();
    UUID getUpdatedById();
    String getUpdatedByName();
}
