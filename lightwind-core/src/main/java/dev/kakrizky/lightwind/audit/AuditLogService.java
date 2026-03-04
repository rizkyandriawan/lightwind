package dev.kakrizky.lightwind.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kakrizky.lightwind.auth.LightUser;
import dev.kakrizky.lightwind.entity.LightEntity;
import dev.kakrizky.lightwind.filter.RequestIdFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

/**
 * Service for recording entity change history.
 *
 * <p>Used automatically by LightCrudService when audit logging is enabled.
 * Can also be used directly for custom audit logging.</p>
 */
@ApplicationScoped
public class AuditLogService {

    @Inject
    ObjectMapper objectMapper;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void log(AuditAction action, LightEntity<?, ?> entity, Object previousDto, Object newDto, LightUser user) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entity.getClass().getSimpleName());
        entry.setEntityId(entity.getId());
        entry.setAction(action);
        entry.setRequestId(RequestIdFilter.getCurrentRequestId());

        if (user != null) {
            entry.setUserId(user.userId());
            entry.setUserName(user.userName());
        }

        try {
            if (previousDto != null) {
                entry.setPreviousValue(objectMapper.writeValueAsString(previousDto));
            }
            if (newDto != null) {
                entry.setNewValue(objectMapper.writeValueAsString(newDto));
            }
        } catch (Exception ignored) {
            // Don't fail the main operation if audit serialization fails
        }

        entry.persist();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void log(AuditAction action, String entityType, UUID entityId, LightUser user) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setRequestId(RequestIdFilter.getCurrentRequestId());

        if (user != null) {
            entry.setUserId(user.userId());
            entry.setUserName(user.userName());
        }

        entry.persist();
    }
}
