package dev.kakrizky.lightwind.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Application-scoped service wrapping the Redis data source for
 * cache get / put / evict operations.
 *
 * <p>Values are stored as JSON strings using Jackson {@link ObjectMapper}.</p>
 */
@ApplicationScoped
public class LightCacheService {

    private static final Logger LOG = Logger.getLogger(LightCacheService.class);

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    public LightCacheService(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.keyCommands = redisDataSource.key(String.class);
    }

    /**
     * Retrieves a cached value, deserializing the stored JSON into the requested type.
     *
     * @param key  the cache key
     * @param type the target class
     * @param <T>  the target type
     * @return the cached value, or empty if not present or on deserialization error
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = valueCommands.get(key);
            if (json == null) {
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, type);
            return Optional.of(value);
        } catch (Exception e) {
            LOG.warnv("Failed to read cache key {0}: {1}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stores a value in Redis, serialized as JSON, with the given TTL.
     *
     * @param key        the cache key
     * @param value      the value to cache
     * @param ttlSeconds time-to-live in seconds
     */
    public void put(String key, Object value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            SetArgs args = new SetArgs().ex(Duration.ofSeconds(ttlSeconds));
            valueCommands.set(key, json, args);
        } catch (Exception e) {
            LOG.warnv("Failed to write cache key {0}: {1}", key, e.getMessage());
        }
    }

    /**
     * Removes a single cache entry.
     *
     * @param key the cache key to evict
     */
    public void evict(String key) {
        try {
            keyCommands.del(key);
        } catch (Exception e) {
            LOG.warnv("Failed to evict cache key {0}: {1}", key, e.getMessage());
        }
    }

    /**
     * Removes all cache entries whose key starts with the given prefix.
     *
     * <p>Uses Redis {@code KEYS} command to locate matching keys, then
     * deletes them in bulk. For very large key-spaces consider using
     * SCAN instead.</p>
     *
     * @param prefix the key prefix to match (e.g. {@code "lightwind:users:"})
     */
    public void evictByPrefix(String prefix) {
        try {
            List<String> keys = keyCommands.keys(prefix + "*");
            if (!keys.isEmpty()) {
                keyCommands.del(keys.toArray(new String[0]));
                LOG.debugv("Evicted {0} keys with prefix {1}", keys.size(), prefix);
            }
        } catch (Exception e) {
            LOG.warnv("Failed to evict keys with prefix {0}: {1}", prefix, e.getMessage());
        }
    }
}
