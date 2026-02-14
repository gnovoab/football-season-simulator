package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.*;
import com.example.footballseasonsimulator.service.SimulationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MatchController.
 * Uses real services and data - no mocking.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulationService simulationService;

    @Test
    @DisplayName("GET /api/v1/matches/{id} should return match details when match exists")
    void getMatchShouldReturnMatchDetailsWhenExists() throws Exception {
        // Wait for a current fixture to be available (matches in progress or completed)
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Fixture fixture = simulationService.getCurrentFixture("premier-league");
                    return fixture != null && !fixture.matches().isEmpty();
                });

        Fixture fixture = simulationService.getCurrentFixture("premier-league");
        String matchId = fixture.matches().get(0).getId();

        mockMvc.perform(get("/api/v1/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId))
                .andExpect(jsonPath("$.homeTeam").exists())
                .andExpect(jsonPath("$.awayTeam").exists())
                .andExpect(jsonPath("$.homeScore").isNumber())
                .andExpect(jsonPath("$.awayScore").isNumber())
                .andExpect(jsonPath("$.phase").exists())
                .andExpect(jsonPath("$.stats").exists());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id}/events should return events when match exists")
    void getMatchEventsShouldReturnEventsWhenExists() throws Exception {
        // Wait for a current fixture to be available
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Fixture fixture = simulationService.getCurrentFixture("premier-league");
                    return fixture != null && !fixture.matches().isEmpty();
                });

        Fixture fixture = simulationService.getCurrentFixture("premier-league");
        String matchId = fixture.matches().get(0).getId();

        mockMvc.perform(get("/api/v1/matches/" + matchId + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id}/events/significant should return significant events when match exists")
    void getSignificantEventsShouldReturnEventsWhenExists() throws Exception {
        // Wait for a current fixture to be available
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Fixture fixture = simulationService.getCurrentFixture("premier-league");
                    return fixture != null && !fixture.matches().isEmpty();
                });

        Fixture fixture = simulationService.getCurrentFixture("premier-league");
        String matchId = fixture.matches().get(0).getId();

        mockMvc.perform(get("/api/v1/matches/" + matchId + "/events/significant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id} should return 404 for non-existent match")
    void getMatchShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/v1/matches/non-existent-match-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id} should return 404 for random UUID")
    void getMatchShouldReturn404ForRandomUuid() throws Exception {
        mockMvc.perform(get("/api/v1/matches/12345678-1234-1234-1234-123456789012"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id}/events should return 404 for non-existent match")
    void getMatchEventsShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/v1/matches/non-existent/events"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id}/events/significant should return 404 for non-existent")
    void getSignificantEventsShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/v1/matches/non-existent/events/significant"))
                .andExpect(status().isNotFound());
    }

    @Nested
    @DisplayName("DTO Tests")
    class DtoTests {

        @Test
        @DisplayName("MatchStatsDTO should calculate stats correctly")
        void matchStatsDtoShouldCalculateStatsCorrectly() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add various events
            match.addEvent(MatchEvent.withPlayer(10, 0, FootballEventType.SHOT_ON_TARGET, home, null, "Shot"));
            match.addEvent(MatchEvent.withPlayer(15, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(20, 0, FootballEventType.SHOT_OFF_TARGET, away, null, "Shot wide"));
            match.addEvent(MatchEvent.withPlayer(25, 0, FootballEventType.CORNER_KICK, home, null, "Corner"));
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.FOUL, away, null, "Foul"));
            match.addEvent(MatchEvent.withPlayer(35, 0, FootballEventType.YELLOW_CARD, away, null, "Yellow"));

            MatchController.MatchStatsDTO stats = MatchController.MatchStatsDTO.calculate(match);

            assertThat(stats.homeShots()).isEqualTo(2); // SHOT_ON_TARGET + GOAL
            assertThat(stats.homeShotsOnTarget()).isEqualTo(2);
            assertThat(stats.awayShots()).isEqualTo(1); // SHOT_OFF_TARGET
            assertThat(stats.awayShotsOnTarget()).isZero();
            assertThat(stats.homeCorners()).isEqualTo(1);
            assertThat(stats.awayFouls()).isEqualTo(1);
            assertThat(stats.awayYellowCards()).isEqualTo(1);
        }

        @Test
        @DisplayName("MatchStatsDTO should calculate red cards correctly")
        void matchStatsDtoShouldCalculateRedCardsCorrectly() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add red card events
            match.addEvent(MatchEvent.withPlayer(50, 0, FootballEventType.RED_CARD, home, null, "Red card!"));
            match.addEvent(MatchEvent.withPlayer(70, 0, FootballEventType.RED_CARD, away, null, "Red card!"));
            match.addEvent(MatchEvent.withPlayer(80, 0, FootballEventType.RED_CARD, away, null, "Another red!"));

            MatchController.MatchStatsDTO stats = MatchController.MatchStatsDTO.calculate(match);

            assertThat(stats.homeRedCards()).isEqualTo(1);
            assertThat(stats.awayRedCards()).isEqualTo(2);
        }

        @Test
        @DisplayName("MatchStatsDTO should calculate penalty scored correctly")
        void matchStatsDtoShouldCalculatePenaltyScoredCorrectly() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add penalty scored events
            match.addEvent(MatchEvent.withPlayer(45, 0, FootballEventType.PENALTY_SCORED, home, null, "Penalty!"));
            match.addEvent(MatchEvent.withPlayer(60, 0, FootballEventType.PENALTY_SCORED, away, null, "Penalty!"));

            MatchController.MatchStatsDTO stats = MatchController.MatchStatsDTO.calculate(match);

            // PENALTY_SCORED counts as shot and shot on target
            assertThat(stats.homeShots()).isEqualTo(1);
            assertThat(stats.homeShotsOnTarget()).isEqualTo(1);
            assertThat(stats.awayShots()).isEqualTo(1);
            assertThat(stats.awayShotsOnTarget()).isEqualTo(1);
        }

        @Test
        @DisplayName("MatchStatsDTO should calculate second yellow correctly")
        void matchStatsDtoShouldCalculateSecondYellowCorrectly() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add second yellow events
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.YELLOW_CARD, home, null, "Yellow"));
            match.addEvent(MatchEvent.withPlayer(55, 0, FootballEventType.SECOND_YELLOW, home, null, "Second yellow!"));
            match.addEvent(MatchEvent.withPlayer(40, 0, FootballEventType.YELLOW_CARD, away, null, "Yellow"));

            MatchController.MatchStatsDTO stats = MatchController.MatchStatsDTO.calculate(match);

            // SECOND_YELLOW counts as yellow card
            assertThat(stats.homeYellowCards()).isEqualTo(2); // YELLOW_CARD + SECOND_YELLOW
            assertThat(stats.awayYellowCards()).isEqualTo(1);
        }

        @Test
        @DisplayName("MatchStatsDTO should handle all event types for away team")
        void matchStatsDtoShouldHandleAllEventTypesForAwayTeam() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add all event types for away team
            match.addEvent(MatchEvent.withPlayer(5, 0, FootballEventType.SHOT_ON_TARGET, away, null, "Shot"));
            match.addEvent(MatchEvent.withPlayer(10, 0, FootballEventType.GOAL, away, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(15, 0, FootballEventType.PENALTY_SCORED, away, null, "Penalty!"));
            match.addEvent(MatchEvent.withPlayer(20, 0, FootballEventType.SHOT_OFF_TARGET, away, null, "Shot wide"));
            match.addEvent(MatchEvent.withPlayer(25, 0, FootballEventType.CORNER_KICK, away, null, "Corner"));
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.FOUL, away, null, "Foul"));
            match.addEvent(MatchEvent.withPlayer(35, 0, FootballEventType.YELLOW_CARD, away, null, "Yellow"));
            match.addEvent(MatchEvent.withPlayer(40, 0, FootballEventType.SECOND_YELLOW, away, null, "Second yellow"));
            match.addEvent(MatchEvent.withPlayer(45, 0, FootballEventType.RED_CARD, away, null, "Red card"));

            MatchController.MatchStatsDTO stats = MatchController.MatchStatsDTO.calculate(match);

            assertThat(stats.awayShots()).isEqualTo(4); // SHOT_ON_TARGET + GOAL + PENALTY_SCORED + SHOT_OFF_TARGET
            assertThat(stats.awayShotsOnTarget()).isEqualTo(3); // SHOT_ON_TARGET + GOAL + PENALTY_SCORED
            assertThat(stats.awayCorners()).isEqualTo(1);
            assertThat(stats.awayFouls()).isEqualTo(1);
            assertThat(stats.awayYellowCards()).isEqualTo(2); // YELLOW_CARD + SECOND_YELLOW
            assertThat(stats.awayRedCards()).isEqualTo(1);
            // Home stats should be zero
            assertThat(stats.homeShots()).isZero();
            assertThat(stats.homeCorners()).isZero();
        }

        @Test
        @DisplayName("MatchStatsDTO should handle non-stat events gracefully")
        void matchStatsDtoShouldHandleNonStatEventsGracefully() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add events that don't affect stats (default case in switch)
            match.addEvent(MatchEvent.withPlayer(0, 0, FootballEventType.KICK_OFF, null, null, "Kick off!"));
            match.addEvent(MatchEvent.withPlayer(45, 0, FootballEventType.HALF_TIME, null, null, "Half time"));
            match.addEvent(MatchEvent.withPlayer(90, 0, FootballEventType.FULL_TIME, null, null, "Full time"));

            MatchController.MatchStatsDTO stats = MatchController.MatchStatsDTO.calculate(match);

            // All stats should be zero
            assertThat(stats.homeShots()).isZero();
            assertThat(stats.awayShots()).isZero();
            assertThat(stats.homeCorners()).isZero();
            assertThat(stats.awayCorners()).isZero();
        }

        @Test
        @DisplayName("TeamInfoDTO should map team correctly")
        void teamInfoDtoShouldMapTeamCorrectly() {
            Team team = createTeam("arsenal", "Arsenal");
            MatchController.TeamInfoDTO dto = MatchController.TeamInfoDTO.from(team, "WWDLW");

            assertThat(dto.id()).isEqualTo("arsenal");
            assertThat(dto.name()).isEqualTo("Arsenal");
            assertThat(dto.shortName()).isEqualTo("ARS");
            assertThat(dto.form()).isEqualTo("WWDLW");
        }

        @Test
        @DisplayName("EventDTO should map event correctly")
        void eventDtoShouldMapEventCorrectly() {
            MatchEvent event = MatchEvent.withPlayer(45, 2, FootballEventType.GOAL, null, null, "Goal!");
            MatchController.EventDTO dto = MatchController.EventDTO.from(event);

            assertThat(dto.displayTime()).isEqualTo("45+2'");
            assertThat(dto.type()).isEqualTo("GOAL");
            assertThat(dto.description()).isEqualTo("Goal!");
        }

        @Test
        @DisplayName("MatchDetailDTO should map match correctly")
        void matchDetailDtoShouldMapMatchCorrectly() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 5, home, away);
            match.setPhase(MatchPhase.SECOND_HALF);
            match.setMatchMinute(67);

            MatchController.MatchDetailDTO dto = MatchController.MatchDetailDTO.from(match, "WWDLW", "LDWWW");

            assertThat(dto.leagueId()).isEqualTo("test-league");
            assertThat(dto.season()).isEqualTo(2025);
            assertThat(dto.matchweek()).isEqualTo(5);
            assertThat(dto.phase()).isEqualTo("SECOND_HALF");
            assertThat(dto.homeTeam().form()).isEqualTo("WWDLW");
            assertThat(dto.awayTeam().form()).isEqualTo("LDWWW");
        }

        @Test
        @DisplayName("MatchDetailDTO should filter only significant events")
        void matchDetailDtoShouldFilterOnlySignificantEvents() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add mix of significant and non-significant events
            // Non-significant: KICK_OFF, SHOT_ON_TARGET, HALF_TIME, CORNER_KICK
            // Significant: GOAL, YELLOW_CARD, RED_CARD
            match.addEvent(MatchEvent.withPlayer(0, 0, FootballEventType.KICK_OFF, null, null, "Kick off"));
            match.addEvent(MatchEvent.withPlayer(10, 0, FootballEventType.SHOT_ON_TARGET, home, null, "Shot"));
            match.addEvent(MatchEvent.withPlayer(15, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.YELLOW_CARD, away, null, "Yellow"));
            match.addEvent(MatchEvent.withPlayer(35, 0, FootballEventType.CORNER_KICK, home, null, "Corner"));
            match.addEvent(MatchEvent.withPlayer(45, 0, FootballEventType.HALF_TIME, null, null, "Half time"));
            match.addEvent(MatchEvent.withPlayer(60, 0, FootballEventType.RED_CARD, away, null, "Red card"));

            MatchController.MatchDetailDTO dto = MatchController.MatchDetailDTO.from(match, "", "");

            // Only significant events should be included (GOAL, YELLOW_CARD, RED_CARD)
            // KICK_OFF, SHOT_ON_TARGET, CORNER_KICK, HALF_TIME are not significant
            assertThat(dto.events()).hasSize(3);
            assertThat(dto.events().stream().map(MatchController.EventDTO::type))
                .containsExactly("GOAL", "YELLOW_CARD", "RED_CARD");
        }

        @Test
        @DisplayName("MatchDetailDTO should include match id and scores")
        void matchDetailDtoShouldIncludeMatchIdAndScores() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            // Add goals to update scores
            match.addEvent(MatchEvent.withPlayer(10, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(20, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.GOAL, away, null, "Goal!"));

            MatchController.MatchDetailDTO dto = MatchController.MatchDetailDTO.from(match, "", "");

            assertThat(dto.id()).isNotNull();
            assertThat(dto.homeScore()).isEqualTo(2);
            assertThat(dto.awayScore()).isEqualTo(1);
        }

        @Test
        @DisplayName("MatchDetailDTO should include time display")
        void matchDetailDtoShouldIncludeTimeDisplay() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);
            match.setPhase(MatchPhase.FIRST_HALF);
            match.setMatchMinute(35);

            MatchController.MatchDetailDTO dto = MatchController.MatchDetailDTO.from(match, "", "");

            assertThat(dto.timeDisplay()).isEqualTo("35'");
        }

        @Test
        @DisplayName("EventDTO should include team and player info")
        void eventDtoShouldIncludeTeamAndPlayerInfo() {
            Team team = createTeam("arsenal", "Arsenal");
            Player player = new Player("saka", "Bukayo Saka", Position.MIDFIELDER, 7, 85);
            MatchEvent event = MatchEvent.withPlayer(25, 0, FootballEventType.GOAL, team, player, "Saka scores!");
            MatchController.EventDTO dto = MatchController.EventDTO.from(event);

            assertThat(dto.teamId()).isEqualTo("arsenal");
            assertThat(dto.playerName()).isEqualTo("Bukayo Saka");
            assertThat(dto.description()).isEqualTo("Saka scores!");
        }

        @Test
        @DisplayName("EventDTO should have unique id")
        void eventDtoShouldHaveUniqueId() {
            MatchEvent event1 = MatchEvent.withPlayer(10, 0, FootballEventType.GOAL, null, null, "Goal 1");
            MatchEvent event2 = MatchEvent.withPlayer(20, 0, FootballEventType.GOAL, null, null, "Goal 2");

            MatchController.EventDTO dto1 = MatchController.EventDTO.from(event1);
            MatchController.EventDTO dto2 = MatchController.EventDTO.from(event2);

            assertThat(dto1.id()).isNotNull();
            assertThat(dto2.id()).isNotNull();
            assertThat(dto1.id()).isNotEqualTo(dto2.id());
        }

        @Test
        @DisplayName("TeamInfoDTO should handle empty form")
        void teamInfoDtoShouldHandleEmptyForm() {
            Team team = createTeam("chelsea", "Chelsea");
            MatchController.TeamInfoDTO dto = MatchController.TeamInfoDTO.from(team, "");

            assertThat(dto.form()).isEmpty();
        }

        @Test
        @DisplayName("TeamInfoDTO should include badge URL")
        void teamInfoDtoShouldIncludeBadgeUrl() {
            Team team = createTeam("liverpool", "Liverpool");
            MatchController.TeamInfoDTO dto = MatchController.TeamInfoDTO.from(team, "WWWWW");

            assertThat(dto.badgeUrl()).isEqualTo("/badge.png");
        }

        private Team createTeam(String id, String name) {
            return new Team(id, name, "ARS", "/badge.png",
                    new TeamStrength(80, 80, 80, 80), List.of());
        }
    }
}

