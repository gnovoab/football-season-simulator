package com.example.footballseasonsimulator.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine.
 *
 * <p>Caching Strategy:
 * <ul>
 *   <li><b>fixtures</b>: Short TTL (10 seconds) - Current and next fixture data</li>
 * </ul>
 *
 * <p>Note: Fixtures are cached with a short TTL because they change
 * as matchweeks progress. The 10-second TTL reduces repeated lookups
 * while maintaining near real-time accuracy.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache name for fixtures.
     */
    public static final String CACHE_FIXTURES = "fixtures";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Fixtures cache - short TTL as fixtures change during simulation
        cacheManager.registerCustomCache(CACHE_FIXTURES,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.SECONDS)
                        .maximumSize(100)
                        .build());

        return cacheManager;
    }
}

