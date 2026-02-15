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
 * Integration tests for StatisticsController.
 * Uses real services and data - no mocking.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StatisticsControllerTest {

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
    @DisplayName("GET /api/v1/statistics/{leagueId}/top-scorers")
    class TopScorersTests {

        @Test
        @DisplayName("Should return top scorers list for valid league")
        void shouldReturnTopScorersForValidLeague() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/premier-league/top-scorers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return empty list when no matches completed")
        void shouldReturnEmptyListWhenNoMatchesCompleted() throws Exception {
            // At startup, no matches are completed yet
            mockMvc.perform(get("/api/v1/statistics/premier-league/top-scorers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/premier-league/top-scorers")
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return 400 for invalid league ID format")
        void shouldReturn400ForInvalidLeagueIdFormat() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/INVALID_LEAGUE/top-scorers"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/statistics/{leagueId}/teams/{teamId}")
    class TeamStatsTests {

        @Test
        @DisplayName("Should return team stats for valid team")
        void shouldReturnTeamStatsForValidTeam() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/premier-league/teams/arsenal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.teamId").value("arsenal"))
                    .andExpect(jsonPath("$.teamName").isNotEmpty())
                    .andExpect(jsonPath("$.played").isNumber())
                    .andExpect(jsonPath("$.won").isNumber())
                    .andExpect(jsonPath("$.drawn").isNumber())
                    .andExpect(jsonPath("$.lost").isNumber())
                    .andExpect(jsonPath("$.goalsFor").isNumber())
                    .andExpect(jsonPath("$.goalsAgainst").isNumber())
                    .andExpect(jsonPath("$.points").isNumber());
        }

        @Test
        @DisplayName("Should return 404 for non-existent team")
        void shouldReturn404ForNonExistentTeam() throws Exception {
            // Team ID that doesn't exist in any league
            mockMvc.perform(get("/api/v1/statistics/premier-league/teams/xyz-team-not-exists"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for invalid team ID format")
        void shouldReturn400ForInvalidTeamIdFormat() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/premier-league/teams/INVALID_TEAM"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return stats for Chelsea")
        void shouldReturnStatsForChelsea() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/premier-league/teams/chelsea"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.teamId").value("chelsea"))
                    .andExpect(jsonPath("$.teamName").isNotEmpty());
        }

        @Test
        @DisplayName("Should return stats for La Liga team")
        void shouldReturnStatsForLaLigaTeam() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/la-liga/teams/barcelona"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.teamId").value("barcelona"))
                    .andExpect(jsonPath("$.teamName").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/statistics/{leagueId}/summary")
    class LeagueSummaryTests {

        @Test
        @DisplayName("Should return league summary for valid league")
        void shouldReturnLeagueSummaryForValidLeague() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/premier-league/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.leagueId").value("premier-league"))
                    .andExpect(jsonPath("$.totalMatches").isNumber())
                    .andExpect(jsonPath("$.totalGoals").isNumber())
                    .andExpect(jsonPath("$.avgGoalsPerMatch").isNumber())
                    .andExpect(jsonPath("$.homeWins").isNumber())
                    .andExpect(jsonPath("$.awayWins").isNumber())
                    .andExpect(jsonPath("$.draws").isNumber());
        }

        @Test
        @DisplayName("Should return summary for all leagues")
        void shouldReturnSummaryForAllLeagues() throws Exception {
            String[] leagues = {"premier-league", "la-liga", "serie-a", "bundesliga", "ligue-1"};
            for (String league : leagues) {
                mockMvc.perform(get("/api/v1/statistics/" + league + "/summary"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.leagueId").value(league));
            }
        }

        @Test
        @DisplayName("Should return 400 for invalid league ID format")
        void shouldReturn400ForInvalidLeagueIdFormat() throws Exception {
            mockMvc.perform(get("/api/v1/statistics/INVALID_LEAGUE/summary"))
                    .andExpect(status().isBadRequest());
        }
    }
}

