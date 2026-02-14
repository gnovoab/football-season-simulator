package com.example.footballseasonsimulator.service;

import com.example.footballseasonsimulator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandingsServiceTest {

    private StandingsService standingsService;
    private League testLeague;
    private Team team1;
    private Team team2;

    @BeforeEach
    void setUp() {
        standingsService = new StandingsService();
        team1 = createTestTeam("team1", "Team One");
        team2 = createTestTeam("team2", "Team Two");
        testLeague = new League("test-league", "Test League", "Test", "/logo.png", List.of(team1, team2));
        standingsService.clearAll();
    }

    @Test
    @DisplayName("Should initialize standings for all teams")
    void shouldInitializeStandings() {
        standingsService.initializeSeason(testLeague, 2025);
        List<Standing> standings = standingsService.getStandings("test-league", 2025);
        assertEquals(2, standings.size());
        for (Standing s : standings) {
            assertEquals(0, s.getPlayed());
            assertEquals(0, s.getPoints());
        }
    }

    @Test
    @DisplayName("Should update standings after home win")
    void shouldUpdateStandingsAfterHomeWin() {
        standingsService.initializeSeason(testLeague, 2025);
        Match match = createMatchWithScore(team1, team2, 2, 0);
        standingsService.updateFromMatch("test-league", 2025, match);

        Standing home = standingsService.getTeamStanding("test-league", 2025, team1.id());
        Standing away = standingsService.getTeamStanding("test-league", 2025, team2.id());

        assertEquals(3, home.getPoints());
        assertEquals(1, home.getWon());
        assertEquals(0, away.getPoints());
        assertEquals(1, away.getLost());
    }

    @Test
    @DisplayName("Should update standings after draw")
    void shouldUpdateStandingsAfterDraw() {
        standingsService.initializeSeason(testLeague, 2025);
        Match match = createMatchWithScore(team1, team2, 1, 1);
        standingsService.updateFromMatch("test-league", 2025, match);

        Standing home = standingsService.getTeamStanding("test-league", 2025, team1.id());
        assertEquals(1, home.getPoints());
        assertEquals(1, home.getDrawn());
    }

    @Test
    @DisplayName("Should track goal difference")
    void shouldTrackGoalDifference() {
        standingsService.initializeSeason(testLeague, 2025);
        Match match = createMatchWithScore(team1, team2, 3, 1);
        standingsService.updateFromMatch("test-league", 2025, match);

        Standing home = standingsService.getTeamStanding("test-league", 2025, team1.id());
        assertEquals(3, home.getGoalsFor());
        assertEquals(1, home.getGoalsAgainst());
        assertEquals(2, home.getGoalDifference());
    }

    @Test
    @DisplayName("Should return empty list for non-existent league")
    void shouldReturnEmptyForNonExistentLeague() {
        List<Standing> standings = standingsService.getStandings("non-existent", 2025);
        assertTrue(standings.isEmpty());
    }

    @Test
    @DisplayName("clearAll should remove all standings")
    void clearAllShouldRemoveAllStandings() {
        standingsService.initializeSeason(testLeague, 2025);
        standingsService.clearAll();
        assertTrue(standingsService.getStandings("test-league", 2025).isEmpty());
    }

    @Test
    @DisplayName("Should update standings after away win")
    void shouldUpdateStandingsAfterAwayWin() {
        standingsService.initializeSeason(testLeague, 2025);
        Match match = createMatchWithScore(team1, team2, 0, 3);
        standingsService.updateFromMatch("test-league", 2025, match);

        Standing home = standingsService.getTeamStanding("test-league", 2025, team1.id());
        Standing away = standingsService.getTeamStanding("test-league", 2025, team2.id());

        assertEquals(0, home.getPoints());
        assertEquals(1, home.getLost());
        assertEquals(3, away.getPoints());
        assertEquals(1, away.getWon());
    }

    @Test
    @DisplayName("Should track form correctly")
    void shouldTrackFormCorrectly() {
        standingsService.initializeSeason(testLeague, 2025);

        // Win
        Match match1 = createMatchWithScore(team1, team2, 2, 0);
        standingsService.updateFromMatch("test-league", 2025, match1);

        Standing standing = standingsService.getTeamStanding("test-league", 2025, team1.id());
        String form = standing.getFormString();
        assertTrue(form.contains("W"), "Form should contain W for win");
    }

    @Test
    @DisplayName("Should sort standings by points")
    void shouldSortStandingsByPoints() {
        standingsService.initializeSeason(testLeague, 2025);

        // Team 2 wins
        Match match = createMatchWithScore(team1, team2, 0, 1);
        standingsService.updateFromMatch("test-league", 2025, match);

        List<Standing> standings = standingsService.getStandings("test-league", 2025);
        assertEquals(team2.id(), standings.get(0).getTeamId(), "Team 2 should be first with 3 points");
        assertEquals(team1.id(), standings.get(1).getTeamId(), "Team 1 should be second with 0 points");
    }

    @Test
    @DisplayName("Should return null for non-existent team standing")
    void shouldReturnNullForNonExistentTeam() {
        standingsService.initializeSeason(testLeague, 2025);
        Standing standing = standingsService.getTeamStanding("test-league", 2025, "non-existent");
        assertNull(standing);
    }

    @Test
    @DisplayName("Should handle multiple matches")
    void shouldHandleMultipleMatches() {
        standingsService.initializeSeason(testLeague, 2025);

        Match match1 = createMatchWithScore(team1, team2, 2, 0);
        Match match2 = createMatchWithScore(team2, team1, 1, 1);

        standingsService.updateFromMatch("test-league", 2025, match1);
        standingsService.updateFromMatch("test-league", 2025, match2);

        Standing team1Standing = standingsService.getTeamStanding("test-league", 2025, team1.id());
        assertEquals(2, team1Standing.getPlayed());
        assertEquals(4, team1Standing.getPoints()); // 3 for win + 1 for draw
    }

    private Team createTestTeam(String id, String name) {
        List<Player> players = List.of(
                new Player("gk-" + id, "GK", Position.GOALKEEPER, 1, 75),
                new Player("fwd-" + id, "FWD", Position.FORWARD, 9, 75)
        );
        return new Team(id, name, "TST", "/badge.png", new TeamStrength(75, 75, 75, 75), players);
    }

    @Test
    @DisplayName("Should handle update for non-existent league")
    void shouldHandleUpdateForNonExistentLeague() {
        Match match = createMatchWithScore(team1, team2, 1, 0);
        // Should not throw exception
        standingsService.updateFromMatch("non-existent", 2025, match);
    }

    @Test
    @DisplayName("Should handle update for non-existent season")
    void shouldHandleUpdateForNonExistentSeason() {
        standingsService.initializeSeason(testLeague, 2025);
        Match match = createMatchWithScore(team1, team2, 1, 0);
        // Should not throw exception
        standingsService.updateFromMatch("test-league", 2024, match);
    }

    @Test
    @DisplayName("Should return null for non-existent season")
    void shouldReturnNullForNonExistentSeason() {
        standingsService.initializeSeason(testLeague, 2025);
        Standing standing = standingsService.getTeamStanding("test-league", 2024, team1.id());
        assertNull(standing);
    }

    @Test
    @DisplayName("Should calculate positions correctly after multiple matches")
    void shouldCalculatePositionsCorrectly() {
        Team team3 = createTestTeam("team3", "Team Three");
        League threeTeamLeague = new League("test-league", "Test League", "Test", "/logo.png",
                List.of(team1, team2, team3));
        standingsService.initializeSeason(threeTeamLeague, 2025);

        // Team 1 wins against team 2
        standingsService.updateFromMatch("test-league", 2025, createMatchWithScore(team1, team2, 2, 0));
        // Team 3 wins against team 1
        standingsService.updateFromMatch("test-league", 2025, createMatchWithScore(team3, team1, 1, 0));
        // Team 3 wins against team 2
        standingsService.updateFromMatch("test-league", 2025, createMatchWithScore(team3, team2, 3, 0));

        List<Standing> standings = standingsService.getStandings("test-league", 2025);
        assertEquals(team3.id(), standings.get(0).getTeamId()); // 6 points
        assertEquals(team1.id(), standings.get(1).getTeamId()); // 3 points
        assertEquals(team2.id(), standings.get(2).getTeamId()); // 0 points
    }

    private Match createMatchWithScore(Team home, Team away, int homeScore, int awayScore) {
        Match match = new Match("test-league", 2025, 1, home, away);
        match.setPhase(MatchPhase.FULL_TIME);
        for (int i = 0; i < homeScore; i++) {
            match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.GOAL, home, null, "Goal"));
        }
        for (int i = 0; i < awayScore; i++) {
            match.addEvent(MatchEvent.withPlayer(60, 0, FootballEventType.GOAL, away, null, "Goal"));
        }
        return match;
    }
}

