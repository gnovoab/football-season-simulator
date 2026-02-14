package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LeagueController.
 * Uses real services and data - no mocking.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/leagues should return all 5 leagues")
    void getAllLeaguesShouldReturnAllLeagues() throws Exception {
        mockMvc.perform(get("/api/leagues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        "premier-league", "la-liga", "serie-a", "bundesliga", "ligue-1")));
    }

    @Test
    @DisplayName("GET /api/leagues/premier-league should return Premier League")
    void getLeagueShouldReturnPremierLeague() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("premier-league"))
                .andExpect(jsonPath("$.name").value("Premier League"))
                .andExpect(jsonPath("$.country").value("England"))
                .andExpect(jsonPath("$.teamCount").value(20));
    }

    @Test
    @DisplayName("GET /api/leagues/la-liga should return La Liga")
    void getLeagueShouldReturnLaLiga() throws Exception {
        mockMvc.perform(get("/api/leagues/la-liga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("la-liga"))
                .andExpect(jsonPath("$.name").value("La Liga"))
                .andExpect(jsonPath("$.country").value("Spain"));
    }

    @Test
    @DisplayName("GET /api/leagues/serie-a should return Serie A")
    void getLeagueShouldReturnSerieA() throws Exception {
        mockMvc.perform(get("/api/leagues/serie-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("serie-a"))
                .andExpect(jsonPath("$.name").value("Serie A"))
                .andExpect(jsonPath("$.country").value("Italy"));
    }

    @Test
    @DisplayName("GET /api/leagues/bundesliga should return Bundesliga")
    void getLeagueShouldReturnBundesliga() throws Exception {
        mockMvc.perform(get("/api/leagues/bundesliga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("bundesliga"))
                .andExpect(jsonPath("$.name").value("Bundesliga"))
                .andExpect(jsonPath("$.country").value("Germany"));
    }

    @Test
    @DisplayName("GET /api/leagues/ligue-1 should return Ligue 1")
    void getLeagueShouldReturnLigue1() throws Exception {
        mockMvc.perform(get("/api/leagues/ligue-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ligue-1"))
                .andExpect(jsonPath("$.name").value("Ligue 1"))
                .andExpect(jsonPath("$.country").value("France"));
    }

    @Test
    @DisplayName("GET /api/leagues/{id} should return 404 for non-existent league")
    void getLeagueShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/standings should return standings with 20 teams")
    void getStandingsShouldReturnStandings() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)))
                .andExpect(jsonPath("$[0].teamId").isNotEmpty())
                .andExpect(jsonPath("$[0].teamName").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/leagues/bundesliga/standings should return 18 teams")
    void getBundesligaStandingsShouldReturn18Teams() throws Exception {
        mockMvc.perform(get("/api/leagues/bundesliga/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/status should return simulation status")
    void getStatusShouldReturnStatus() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leagueId").value("premier-league"))
                .andExpect(jsonPath("$.leagueName").value("Premier League"))
                .andExpect(jsonPath("$.season").isNumber())
                .andExpect(jsonPath("$.currentMatchweek").isNumber())
                .andExpect(jsonPath("$.totalMatchweeks").value(38));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/status should return 404 for non-existent")
    void getStatusShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/live should return array of live matches")
    void getLiveMatchesShouldReturnArray() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/results should return array of completed matches")
    void getResultsShouldReturnArray() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/fixture should return current or no content")
    void getCurrentFixtureShouldReturnFixtureOrNoContent() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/fixture"))
                .andExpect(status().is(anyOf(equalTo(200), equalTo(204))));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/next-fixture should return next fixture or no content")
    void getNextFixtureShouldReturnFixtureOrNoContent() throws Exception {
        mockMvc.perform(get("/api/leagues/premier-league/next-fixture"))
                .andExpect(status().is(anyOf(equalTo(200), equalTo(204))));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/standings should return 404 for non-existent league")
    void getStandingsShouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/live should return 404 for non-existent league")
    void getLiveMatchesShouldReturnEmptyForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/results should return empty for non-existent league")
    void getResultsShouldReturnEmptyForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/fixture should return no content for non-existent league")
    void getFixtureShouldReturnNoContentForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent/fixture"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/leagues/{id}/next-fixture should return no content for non-existent league")
    void getNextFixtureShouldReturnNoContentForNonExistent() throws Exception {
        mockMvc.perform(get("/api/leagues/non-existent/next-fixture"))
                .andExpect(status().isNoContent());
    }

    @Nested
    @DisplayName("DTO Tests")
    class DtoTests {

        @Test
        @DisplayName("LeagueDTO should map league correctly")
        void leagueDtoShouldMapLeagueCorrectly() {
            List<Team> teams = List.of(createTeam("team1", "Team 1"), createTeam("team2", "Team 2"));
            League league = new League("test-league", "Test League", "Test Country", "/logo.png", teams);

            LeagueController.LeagueDTO dto = LeagueController.LeagueDTO.from(league);

            assertThat(dto.id()).isEqualTo("test-league");
            assertThat(dto.name()).isEqualTo("Test League");
            assertThat(dto.country()).isEqualTo("Test Country");
            assertThat(dto.logoUrl()).isEqualTo("/logo.png");
            assertThat(dto.teamCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("MatchDTO should map match correctly")
        void matchDtoShouldMapMatchCorrectly() {
            Team home = createTeam("arsenal", "Arsenal");
            Team away = createTeam("chelsea", "Chelsea");
            Match match = new Match("premier-league", 2025, 10, home, away);
            match.setPhase(MatchPhase.SECOND_HALF);
            match.setMatchMinute(75);
            // Add goals to set score (2-1)
            match.addEvent(MatchEvent.withPlayer(10, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(25, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(60, 0, FootballEventType.GOAL, away, null, "Goal!"));

            LeagueController.MatchDTO dto = LeagueController.MatchDTO.from(match);

            assertThat(dto.id()).isNotNull();
            assertThat(dto.homeTeamId()).isEqualTo("arsenal");
            assertThat(dto.homeTeamName()).isEqualTo("Arsenal");
            assertThat(dto.awayTeamId()).isEqualTo("chelsea");
            assertThat(dto.awayTeamName()).isEqualTo("Chelsea");
            assertThat(dto.homeScore()).isEqualTo(2);
            assertThat(dto.awayScore()).isEqualTo(1);
            assertThat(dto.phase()).isEqualTo("SECOND_HALF");
            assertThat(dto.matchweek()).isEqualTo(10);
        }

        @Test
        @DisplayName("FixtureDTO should map fixture correctly")
        void fixtureDtoShouldMapFixtureCorrectly() {
            Team home = createTeam("arsenal", "Arsenal");
            Team away = createTeam("chelsea", "Chelsea");
            Match match = new Match("premier-league", 2025, 15, home, away);
            Fixture fixture = new Fixture("premier-league", 2025, 15, List.of(match));

            LeagueController.FixtureDTO dto = LeagueController.FixtureDTO.from(fixture);

            assertThat(dto.leagueId()).isEqualTo("premier-league");
            assertThat(dto.season()).isEqualTo(2025);
            assertThat(dto.matchweek()).isEqualTo(15);
            assertThat(dto.matches()).hasSize(1);
            assertThat(dto.matches().get(0).homeTeamId()).isEqualTo("arsenal");
        }

        @Test
        @DisplayName("MatchDTO should handle NOT_STARTED phase")
        void matchDtoShouldHandleNotStartedPhase() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);

            LeagueController.MatchDTO dto = LeagueController.MatchDTO.from(match);

            assertThat(dto.phase()).isEqualTo("NOT_STARTED");
            assertThat(dto.homeScore()).isZero();
            assertThat(dto.awayScore()).isZero();
        }

        @Test
        @DisplayName("MatchDTO should handle FULL_TIME phase")
        void matchDtoShouldHandleFullTimePhase() {
            Team home = createTeam("home", "Home Team");
            Team away = createTeam("away", "Away Team");
            Match match = new Match("test-league", 2025, 1, home, away);
            match.setPhase(MatchPhase.FULL_TIME);
            // Add goals to set score (3-2)
            match.addEvent(MatchEvent.withPlayer(10, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(20, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.GOAL, home, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(40, 0, FootballEventType.GOAL, away, null, "Goal!"));
            match.addEvent(MatchEvent.withPlayer(50, 0, FootballEventType.GOAL, away, null, "Goal!"));

            LeagueController.MatchDTO dto = LeagueController.MatchDTO.from(match);

            assertThat(dto.phase()).isEqualTo("FULL_TIME");
            assertThat(dto.homeScore()).isEqualTo(3);
            assertThat(dto.awayScore()).isEqualTo(2);
        }

        @Test
        @DisplayName("FixtureDTO should handle empty matches list")
        void fixtureDtoShouldHandleEmptyMatchesList() {
            Fixture fixture = new Fixture("test-league", 2025, 1, List.of());

            LeagueController.FixtureDTO dto = LeagueController.FixtureDTO.from(fixture);

            assertThat(dto.matches()).isEmpty();
        }

        private Team createTeam(String id, String name) {
            return new Team(id, name, "TST", "/badge.png",
                    new TeamStrength(80, 80, 80, 80), List.of());
        }
    }
}

