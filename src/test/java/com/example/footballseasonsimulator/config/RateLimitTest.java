package com.example.footballseasonsimulator.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting functionality.
 * Uses the 'ratelimit' profile which has rate limiting enabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ratelimit")
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        // Clear rate limit buckets before each test
        rateLimitConfig.clearBuckets();
    }

    @Nested
    @DisplayName("RateLimitConfig")
    class RateLimitConfigTests {

        @Test
        @DisplayName("Should create bucket for new client")
        void shouldCreateBucketForNewClient() {
            var bucket = rateLimitConfig.resolveBucket("192.168.1.1");

            assertThat(bucket).isNotNull();
            assertThat(rateLimitConfig.getActiveBucketCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return same bucket for same client")
        void shouldReturnSameBucketForSameClient() {
            var bucket1 = rateLimitConfig.resolveBucket("192.168.1.1");
            var bucket2 = rateLimitConfig.resolveBucket("192.168.1.1");

            assertThat(bucket1).isSameAs(bucket2);
            assertThat(rateLimitConfig.getActiveBucketCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should create different buckets for different clients")
        void shouldCreateDifferentBucketsForDifferentClients() {
            var bucket1 = rateLimitConfig.resolveBucket("192.168.1.1");
            var bucket2 = rateLimitConfig.resolveBucket("192.168.1.2");

            assertThat(bucket1).isNotSameAs(bucket2);
            assertThat(rateLimitConfig.getActiveBucketCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should clear all buckets")
        void shouldClearAllBuckets() {
            rateLimitConfig.resolveBucket("192.168.1.1");
            rateLimitConfig.resolveBucket("192.168.1.2");
            assertThat(rateLimitConfig.getActiveBucketCount()).isEqualTo(2);

            rateLimitConfig.clearBuckets();

            assertThat(rateLimitConfig.getActiveBucketCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("RateLimitFilter")
    class RateLimitFilterTests {

        @Test
        @DisplayName("Should add X-Rate-Limit-Remaining header to API responses")
        void shouldAddRateLimitHeader() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/leagues"))
                    .andExpect(status().isOk())
                    .andReturn();

            String remaining = result.getResponse().getHeader("X-Rate-Limit-Remaining");
            assertThat(remaining).isNotNull();
            assertThat(Integer.parseInt(remaining)).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should not rate limit non-API endpoints")
        void shouldNotRateLimitNonApiEndpoints() throws Exception {
            // Non-API endpoint should not have rate limit header
            MvcResult result = mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andReturn();

            String remaining = result.getResponse().getHeader("X-Rate-Limit-Remaining");
            assertThat(remaining).isNull();
        }

        @Test
        @DisplayName("Should handle X-Forwarded-For header for client IP")
        void shouldHandleXForwardedForHeader() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/leagues")
                            .header("X-Forwarded-For", "10.0.0.1, 192.168.1.1"))
                    .andExpect(status().isOk())
                    .andReturn();

            String remaining = result.getResponse().getHeader("X-Rate-Limit-Remaining");
            assertThat(remaining).isNotNull();
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        void shouldReturn429WhenRateLimitExceeded() throws Exception {
            // Exhaust the burst limit (20 requests per second)
            for (int i = 0; i < 21; i++) {
                mockMvc.perform(get("/api/v1/leagues"));
            }

            // Next request should be rate limited
            mockMvc.perform(get("/api/v1/leagues"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("Too many requests"))
                    .andExpect(header().exists("Retry-After"));
        }
    }
}

