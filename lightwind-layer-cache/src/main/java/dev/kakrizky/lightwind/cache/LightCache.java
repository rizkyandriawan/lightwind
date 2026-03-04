package dev.kakrizky.lightwind.cache;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for Redis-backed caching.
 *
 * <p>On invocation the interceptor checks Redis for a cached result.
 * If found, the cached value is deserialized and returned without executing
 * the method body. If not found, the method executes normally and its
 * result is serialized and stored in Redis with the configured TTL.</p>
 *
 * <p>The cache key is built from the annotation {@link #key()} value
 * combined with the method arguments.</p>
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LightCache {

    /**
     * Custom cache key. If empty, the fully-qualified method name is used.
     */
    @Nonbinding
    String key() default "";

    /**
     * TTL in seconds. A value of -1 means the global default from
     * {@link LightCacheConfig#defaultTtl()} is used.
     */
    @Nonbinding
    long ttl() default -1;
}
