package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PredictionsController.
 * Uses real services and data - no mocking.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PredictionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        // Clear rate limit buckets before each test to avoid 429 errors
        rateLimitConfig.clearBuckets();
    }

    @Nested
    @DisplayName("GET /api/v1/predictions/head-to-head")
    class HeadToHeadTests {

        @Test
        @DisplayName("Should return prediction for valid teams")
        void shouldReturnPredictionForValidTeams() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "premier-league")
                            .param("homeTeamId", "arsenal")
                            .param("awayTeamId", "chelsea"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.homeTeamId").value("arsenal"))
                    .andExpect(jsonPath("$.homeTeamName").value("Arsenal"))
                    .andExpect(jsonPath("$.awayTeamId").value("chelsea"))
                    .andExpect(jsonPath("$.awayTeamName").value("Chelsea"))
                    .andExpect(jsonPath("$.winProbability").exists())
                    .andExpect(jsonPath("$.winProbability.homeWin").isNumber())
                    .andExpect(jsonPath("$.winProbability.draw").isNumber())
                    .andExpect(jsonPath("$.winProbability.awayWin").isNumber())
                    .andExpect(jsonPath("$.expectedGoals").exists())
                    .andExpect(jsonPath("$.expectedGoals.homeXG").isNumber())
                    .andExpect(jsonPath("$.expectedGoals.awayXG").isNumber())
                    .andExpect(jsonPath("$.corners").exists())
                    .andExpect(jsonPath("$.eventLikelihood").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent league")
        void shouldReturn404ForNonExistentLeague() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "non-existent")
                            .param("homeTeamId", "arsenal")
                            .param("awayTeamId", "chelsea"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for non-existent home team")
        void shouldReturn404ForNonExistentHomeTeam() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "premier-league")
                            .param("homeTeamId", "non-existent")
                            .param("awayTeamId", "chelsea"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for non-existent away team")
        void shouldReturn404ForNonExistentAwayTeam() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "premier-league")
                            .param("homeTeamId", "arsenal")
                            .param("awayTeamId", "non-existent"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for invalid league ID format")
        void shouldReturn400ForInvalidLeagueIdFormat() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "INVALID_LEAGUE")
                            .param("homeTeamId", "arsenal")
                            .param("awayTeamId", "chelsea"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return prediction for La Liga teams")
        void shouldReturnPredictionForLaLigaTeams() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "la-liga")
                            .param("homeTeamId", "real-madrid")
                            .param("awayTeamId", "barcelona"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.homeTeamId").value("real-madrid"))
                    .andExpect(jsonPath("$.awayTeamId").value("barcelona"));
        }

        @Test
        @DisplayName("Win probabilities should be valid percentages")
        void winProbabilitiesShouldBeValidPercentages() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "premier-league")
                            .param("homeTeamId", "man-city")
                            .param("awayTeamId", "liverpool"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.winProbability.homeWin").isNumber())
                    .andExpect(jsonPath("$.winProbability.draw").isNumber())
                    .andExpect(jsonPath("$.winProbability.awayWin").isNumber());
        }

        @Test
        @DisplayName("Expected goals should be positive")
        void expectedGoalsShouldBePositive() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "premier-league")
                            .param("homeTeamId", "arsenal")
                            .param("awayTeamId", "tottenham"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expectedGoals.homeXG", greaterThan(0.0)))
                    .andExpect(jsonPath("$.expectedGoals.awayXG", greaterThan(0.0)));
        }

        @Test
        @DisplayName("Event likelihood values should be between 0 and 100")
        void eventLikelihoodValuesShouldBeBetween0And100() throws Exception {
            mockMvc.perform(get("/api/v1/predictions/head-to-head")
                            .param("leagueId", "premier-league")
                            .param("homeTeamId", "arsenal")
                            .param("awayTeamId", "chelsea"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventLikelihood.btts", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.eventLikelihood.over25Goals", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.eventLikelihood.over35Goals", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.eventLikelihood.homeCleanSheet", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.eventLikelihood.awayCleanSheet", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))));
        }
    }
}

