package dev.kakrizky.lightwind.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * CDI interceptor that implements the read-through caching logic
 * driven by the {@link LightCache} annotation.
 */
@LightCache
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class LightCacheInterceptor {

    private static final Logger LOG = Logger.getLogger(LightCacheInterceptor.class);

    @Inject
    LightCacheConfig config;

    @Inject
    LightCacheService cacheService;

    @Inject
    ObjectMapper objectMapper;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        if (!config.enabled()) {
            return ctx.proceed();
        }

        Method method = ctx.getMethod();
        LightCache annotation = method.getAnnotation(LightCache.class);
        if (annotation == null) {
            return ctx.proceed();
        }

        String cacheKey = buildCacheKey(annotation, method, ctx.getParameters());
        Class<?> returnType = method.getReturnType();

        // Check cache
        Optional<?> cached = cacheService.get(cacheKey, returnType);
        if (cached.isPresent()) {
            LOG.debugv("Cache hit for key: {0}", cacheKey);
            return cached.get();
        }

        // Cache miss — execute the method
        LOG.debugv("Cache miss for key: {0}", cacheKey);
        Object result = ctx.proceed();

        if (result != null) {
            long ttl = annotation.ttl() >= 0 ? annotation.ttl() : config.defaultTtl();
            cacheService.put(cacheKey, result, ttl);
        }

        return result;
    }

    private String buildCacheKey(LightCache annotation, Method method, Object[] params) {
        String base;
        if (annotation.key() != null && !annotation.key().isEmpty()) {
            base = annotation.key();
        } else {
            base = method.getDeclaringClass().getName() + "." + method.getName();
        }

        if (params == null || params.length == 0) {
            return config.keyPrefix() + base;
        }

        StringJoiner joiner = new StringJoiner(":");
        joiner.add(config.keyPrefix() + base);
        for (Object param : params) {
            joiner.add(param == null ? "null" : param.toString());
        }
        return joiner.toString();
    }
}
