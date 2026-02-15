package com.example.footballseasonsimulator.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for health indicators.
 * Tests SimulationHealthIndicator and DatabaseHealthIndicator.
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthIndicatorTest {

    @Autowired
    private SimulationHealthIndicator simulationHealthIndicator;

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Nested
    @DisplayName("SimulationHealthIndicator")
    class SimulationHealthIndicatorTests {

        @Test
        @DisplayName("Should return UP status when leagues are active")
        void shouldReturnUpWhenLeaguesActive() {
            Health health = simulationHealthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("activeLeagues");
            assertThat(health.getDetails()).containsKey("totalLeagues");
            assertThat(health.getDetails().get("totalLeagues")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should include league details in health response")
        void shouldIncludeLeagueDetails() {
            Health health = simulationHealthIndicator.health();

            // Check that at least one league is included
            assertThat(health.getDetails()).containsKey("Premier League");
        }

        @Test
        @DisplayName("Should include state, season, and matchweek for each league")
        void shouldIncludeLeagueStateDetails() {
            Health health = simulationHealthIndicator.health();

            @SuppressWarnings("unchecked")
            var premierLeagueDetails = (java.util.Map<String, Object>) health.getDetails().get("Premier League");

            assertThat(premierLeagueDetails).containsKey("state");
            assertThat(premierLeagueDetails).containsKey("season");
            assertThat(premierLeagueDetails).containsKey("matchweek");
        }
    }

    @Nested
    @DisplayName("DatabaseHealthIndicator")
    class DatabaseHealthIndicatorTests {

        @Test
        @DisplayName("Should return UP status when database is connected")
        void shouldReturnUpWhenDatabaseConnected() {
            Health health = databaseHealthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }

        @Test
        @DisplayName("Should include database details in health response")
        void shouldIncludeDatabaseDetails() {
            Health health = databaseHealthIndicator.health();

            assertThat(health.getDetails()).containsKey("database");
            assertThat(health.getDetails()).containsKey("status");
            assertThat(health.getDetails().get("database")).isEqualTo("H2");
            assertThat(health.getDetails().get("status")).isEqualTo("Connected");
        }
    }
}

