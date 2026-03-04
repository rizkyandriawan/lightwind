package dev.kakrizky.lightwind.cache;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-based distributed lock using SET NX EX.
 *
 * <p>Each lock is associated with a unique owner ID (UUID) so that
 * only the holder of the lock can release it. This prevents a
 * process from accidentally unlocking a lock acquired by another process.</p>
 */
@ApplicationScoped
public class LightDistributedLock {

    private static final Logger LOG = Logger.getLogger(LightDistributedLock.class);
    private static final String LOCK_PREFIX = "lightwind:lock:";

    private final ValueCommands<String, String> valueCommands;
    private final Map<String, String> ownedLocks = new ConcurrentHashMap<>();

    @Inject
    public LightDistributedLock(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
    }

    /**
     * Attempts to acquire a distributed lock with the given name.
     *
     * <p>The lock is acquired using Redis {@code SET key value NX EX timeout}.
     * If the key does not exist, it is created with an expiration and the
     * method returns {@code true}. If the key already exists (another
     * process holds the lock), the method returns {@code false}.</p>
     *
     * @param lockName       logical lock name (will be prefixed)
     * @param timeoutSeconds how long the lock is held before auto-expiry
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    public boolean tryLock(String lockName, long timeoutSeconds) {
        String key = LOCK_PREFIX + lockName;
        String ownerId = UUID.randomUUID().toString();

        try {
            SetArgs args = new SetArgs().nx().ex(Duration.ofSeconds(timeoutSeconds));
            valueCommands.set(key, ownerId, args);

            // Verify the lock was actually acquired by reading back
            String stored = valueCommands.get(key);
            if (ownerId.equals(stored)) {
                ownedLocks.put(lockName, ownerId);
                LOG.debugv("Acquired lock {0} with owner {1}", lockName, ownerId);
                return true;
            }

            return false;
        } catch (Exception e) {
            LOG.warnv("Failed to acquire lock {0}: {1}", lockName, e.getMessage());
            return false;
        }
    }

    /**
     * Releases a distributed lock previously acquired via {@link #tryLock}.
     *
     * <p>The lock is only released if the current process is the owner.
     * This is verified by comparing the stored value (owner ID) against
     * the locally recorded owner.</p>
     *
     * @param lockName logical lock name
     */
    public void unlock(String lockName) {
        String key = LOCK_PREFIX + lockName;
        String ownerId = ownedLocks.remove(lockName);

        if (ownerId == null) {
            LOG.warnv("Cannot unlock {0}: no local owner record found", lockName);
            return;
        }

        try {
            String stored = valueCommands.get(key);
            if (ownerId.equals(stored)) {
                valueCommands.getdel(key);
                LOG.debugv("Released lock {0}", lockName);
            } else {
                LOG.warnv("Lock {0} owned by another process, skipping unlock", lockName);
            }
        } catch (Exception e) {
            LOG.warnv("Failed to release lock {0}: {1}", lockName, e.getMessage());
        }
    }
}
