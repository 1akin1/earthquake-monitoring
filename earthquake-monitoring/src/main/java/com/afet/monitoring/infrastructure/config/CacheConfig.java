package com.afet.monitoring.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;
import java.util.Map;

/**
 * Enables Spring's caching abstraction and backs it with Redis.
 *
 * <p>Cache names used across the app:
 * <ul>
 *   <li>{@code earthquakes}    — one entry per id (read-through on GET /{id})</li>
 *   <li>{@code earthquakeList} — the full list as a single entry (shorter TTL, since
 *       any write invalidates it)</li>
 * </ul>
 *
 * <p>Values are stored with the default JDK serializer, which works because the domain
 * model implements {@link java.io.Serializable}. (JSON via
 * {@code GenericJackson2JsonRedisSerializer} is the alternative if human-readable keys
 * are wanted — left out here to avoid wiring Jackson around the immutable domain.)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String EARTHQUAKES = "earthquakes";
    public static final String EARTHQUAKE_LIST = "earthquakeList";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .prefixCacheNameWith("eq:");

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                EARTHQUAKES, base.entryTtl(Duration.ofMinutes(10)),
                EARTHQUAKE_LIST, base.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
