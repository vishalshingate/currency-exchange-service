package com.example.currencyexchangeservice.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .prefixCacheNameWith("my-redis-")
            .entryTtl(Duration.ofSeconds(60))
            .enableTimeToIdle()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Use locking writer to avoid concurrent write races
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(redisConnectionFactory);
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(redisCacheConfiguration)
            .build();

        // Wrap RedisCacheManager with a resilient, fail-open CacheManager
        return new ResilientCacheManager(redisCacheManager);
    }

    /**
     * Decorator CacheManager that swallows runtime cache exceptions and
     * allows the application to continue using the database as the
     * source of truth when Redis is unavailable.
     */
    static class ResilientCacheManager implements CacheManager {
        private final CacheManager delegate;
        // Shared circuit breaker state across all caches
        private final AtomicBoolean isCircuitOpen = new AtomicBoolean(false);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private static final long RETRY_INTERVAL_MS = 5000; // 5 seconds wait after failure

        ResilientCacheManager(CacheManager delegate) {
            this.delegate = delegate;
        }

        @Override
        @Nullable
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            return cache == null ? null : new ResilientCache(cache, isCircuitOpen, lastFailureTime, RETRY_INTERVAL_MS);
        }

        @Override
        public Collection<String> getCacheNames() {
            try {
                return delegate.getCacheNames();
            } catch (RuntimeException ex) {
                // In extreme cases where Redis is completely unavailable at startup
                // or during discovery, degrade gracefully by exposing no caches.
                return Collections.emptyList();
            }
        }
    }

    /**
     * Decorator Cache that absorbs runtime exceptions from the underlying
     * Redis cache implementation. On read failures, it returns null so
     * that @Cacheable methods fall back to the database. On write/evict
     * failures, it simply logs and continues.
     */
    static class ResilientCache implements Cache {
        private final Cache delegate;
        private final AtomicBoolean isCircuitOpen;
        private final AtomicLong lastFailureTime;
        private final long retryIntervalMs;

        ResilientCache(Cache delegate, AtomicBoolean isCircuitOpen, AtomicLong lastFailureTime, long retryIntervalMs) {
            this.delegate = delegate;
            this.isCircuitOpen = isCircuitOpen;
            this.lastFailureTime = lastFailureTime;
            this.retryIntervalMs = retryIntervalMs;
        }

        private boolean shouldTry() {
            if (isCircuitOpen.get()) {
                long now = System.currentTimeMillis();
                if (now - lastFailureTime.get() > retryIntervalMs) {
                    // Half-open: attempt one request
                    // We don't need complex half-open logic; just letting verify logic race is fine for a cache
                    return true;
                }
                return false;
            }
            return true;
        }

        private void reportFailure() {
            isCircuitOpen.set(true);
            lastFailureTime.set(System.currentTimeMillis());
        }

        private void reportSuccess() {
             if (isCircuitOpen.get()) {
                 isCircuitOpen.set(false);
             }
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        @Nullable
        public ValueWrapper get(Object key) {
            if (!shouldTry()) return null;
            try {
                ValueWrapper result = delegate.get(key);
                reportSuccess();
                return result;
            } catch (RuntimeException ex) {
                reportFailure();
                return null;
            }
        }

        @Override
        @Nullable
        public <T> T get(Object key, @Nullable Class<T> type) {
            if (!shouldTry()) return null;
            try {
                T result = delegate.get(key, type);
                reportSuccess();
                return result;
            } catch (RuntimeException ex) {
                reportFailure();
                return null;
            }
        }

        @Override
        @Nullable
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            if (!shouldTry()) {
                 try {
                     return valueLoader.call();
                 } catch (Exception e) {
                     throw new ValueRetrievalException(key, valueLoader, e);
                 }
            }
            try {
                T result = delegate.get(key, valueLoader);
                reportSuccess();
                return result;
            } catch (RuntimeException ex) {
                reportFailure();
                try {
                    return valueLoader.call();
                } catch (Exception loaderEx) {
                    throw new ValueRetrievalException(key, valueLoader, loaderEx);
                }
            }
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            if (!shouldTry()) return;
            try {
                delegate.put(key, value);
                reportSuccess();
            } catch (RuntimeException ex) {
                reportFailure();
            }
        }

        @Override
        @Nullable
        public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
            if (!shouldTry()) return null;
            try {
                ValueWrapper result = delegate.putIfAbsent(key, value);
                reportSuccess();
                return result;
            } catch (RuntimeException ex) {
                reportFailure();
                return null;
            }
        }

        @Override
        public void evict(Object key) {
            if (!shouldTry()) return;
            try {
                delegate.evict(key);
                reportSuccess();
            } catch (RuntimeException ex) {
                reportFailure();
            }
        }

        @Override
        public void clear() {
            if (!shouldTry()) return;
            try {
                delegate.clear();
                reportSuccess();
            } catch (RuntimeException ex) {
                reportFailure();
            }
        }
    }
}
