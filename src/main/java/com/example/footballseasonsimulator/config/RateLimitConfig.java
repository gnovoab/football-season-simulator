package com.example.footballseasonsimulator.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j.
 * Provides token bucket rate limiting per client IP address.
 *
 * <p>This configuration is conditionally enabled based on the {@code rate-limit.enabled} property.
 * When disabled (e.g., in test profile), no rate limiting beans are created.
 */
@Configuration
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    /**
     * Cache of rate limit buckets per client IP.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Default rate limit: 100 requests per minute per IP.
     */
    private static final int DEFAULT_CAPACITY = 100;
    private static final Duration DEFAULT_REFILL_DURATION = Duration.ofMinutes(1);

    /**
     * Burst rate limit: 20 requests per second for burst protection.
     */
    private static final int BURST_CAPACITY = 20;
    private static final Duration BURST_REFILL_DURATION = Duration.ofSeconds(1);

    /**
     * Get or create a rate limit bucket for the given client key.
     *
     * @param clientKey the client identifier (typically IP address)
     * @return the rate limit bucket for this client
     */
    public Bucket resolveBucket(String clientKey) {
        return buckets.computeIfAbsent(clientKey, this::createNewBucket);
    }

    /**
     * Create a new bucket with default rate limits.
     * Uses a two-tier approach:
     * - Long-term limit: 100 requests per minute
     * - Burst limit: 20 requests per second
     */
    private Bucket createNewBucket(String clientKey) {
        Bandwidth longTermLimit = Bandwidth.builder()
                .capacity(DEFAULT_CAPACITY)
                .refillGreedy(DEFAULT_CAPACITY, DEFAULT_REFILL_DURATION)
                .build();

        Bandwidth burstLimit = Bandwidth.builder()
                .capacity(BURST_CAPACITY)
                .refillGreedy(BURST_CAPACITY, BURST_REFILL_DURATION)
                .build();

        return Bucket.builder()
                .addLimit(longTermLimit)
                .addLimit(burstLimit)
                .build();
    }

    /**
     * Get the number of active buckets (for monitoring).
     */
    public int getActiveBucketCount() {
        return buckets.size();
    }

    /**
     * Clear all buckets (useful for testing).
     */
    public void clearBuckets() {
        buckets.clear();
    }
}

