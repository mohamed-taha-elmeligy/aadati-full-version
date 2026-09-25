package com.mts.aadati.configs.caching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class CachingConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory){
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(
                                      RedisSerializer.string()
                                )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(
                                        RedisSerializer.json()
                                )
                )
                .prefixCacheNameWith("aadati::");

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception,
                                            @NonNull Cache cache,
                                            @NonNull Object key) {
                log.warn("Cache GET failed on '{}': {} — falling back to DB", cache.getName(), exception.getMessage());
                log.debug("Cache GET failure details", exception);
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception,
                                            @NonNull Cache cache,
                                            @NonNull Object key,
                                            Object value) {
                log.warn("Cache PUT failed on '{}'", cache.getName(), exception);
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception,
                                              @NonNull Cache cache,
                                              @NonNull Object key) {
                log.error("Cache EVICT failed on '{}' key '{}' — possible stale data!",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception,@NonNull Cache cache) {
                log.error("Cache CLEAR failed on '{}'", cache.getName(), exception);
            }
        };
    }
}
