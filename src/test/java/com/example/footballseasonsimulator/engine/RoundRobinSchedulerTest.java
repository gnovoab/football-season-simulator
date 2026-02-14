package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RoundRobinScheduler.
 * Tests fixture generation using the circle method (Berger tables).
 */
class RoundRobinSchedulerTest {

    private RoundRobinScheduler scheduler;
    private League testLeague;

    @BeforeEach
    void setUp() {
        scheduler = new RoundRobinScheduler();
        testLeague = createTestLeague(20);
    }

    @Test
    @DisplayName("Should generate correct number of fixtures for 20-team league")
    void shouldGenerateCorrectNumberOfFixtures() {
        List<Fixture> fixtures = scheduler.generateSeasonFixtures(testLeague, 2025);

        // 20 teams = 38 matchweeks (19 first half + 19 second half)
        assertEquals(38, fixtures.size(), "Should have 38 matchweeks for 20 teams");
    }

    @Test
    @DisplayName("Should generate correct number of matches per matchweek")
    void shouldGenerateCorrectMatchesPerMatchweek() {
        List<Fixture> fixtures = scheduler.generateSeasonFixtures(testLeague, 2025);

        for (Fixture fixture : fixtures) {
            assertEquals(10, fixture.matches().size(),
                    "Each matchweek should have 10 matches for 20 teams");
        }
    }

    @Test
    @DisplayName("Each team should play exactly 38 matches")
    void eachTeamShouldPlayCorrectNumberOfMatches() {
        List<Fixture> fixtures = scheduler.generateSeasonFixtures(testLeague, 2025);

        Map<String, Integer> matchCounts = new HashMap<>();
        for (Team team : testLeague.teams()) {
            matchCounts.put(team.id(), 0);
        }

        for (Fixture fixture : fixtures) {
            for (Match match : fixture.matches()) {
                matchCounts.merge(match.getHomeTeam().id(), 1, Integer::sum);
                matchCounts.merge(match.getAwayTeam().id(), 1, Integer::sum);
            }
        }

        for (Team team : testLeague.teams()) {
            assertEquals(38, matchCounts.get(team.id()),
                    "Team " + team.name() + " should play 38 matches");
        }
    }

    @Test
    @DisplayName("Each team should play 19 home and 19 away matches")
    void eachTeamShouldHaveBalancedHomeAwayMatches() {
        List<Fixture> fixtures = scheduler.generateSeasonFixtures(testLeague, 2025);

        Map<String, Integer> homeCounts = new HashMap<>();
        Map<String, Integer> awayCounts = new HashMap<>();

        for (Team team : testLeague.teams()) {
            homeCounts.put(team.id(), 0);
            awayCounts.put(team.id(), 0);
        }

        for (Fixture fixture : fixtures) {
            for (Match match : fixture.matches()) {
                homeCounts.merge(match.getHomeTeam().id(), 1, Integer::sum);
                awayCounts.merge(match.getAwayTeam().id(), 1, Integer::sum);
            }
        }

        for (Team team : testLeague.teams()) {
            assertEquals(19, homeCounts.get(team.id()),
                    "Team " + team.name() + " should have 19 home matches");
            assertEquals(19, awayCounts.get(team.id()),
                    "Team " + team.name() + " should have 19 away matches");
        }
    }

    @Test
    @DisplayName("Each team should play every other team exactly twice")
    void eachTeamShouldPlayEveryOtherTeamTwice() {
        List<Fixture> fixtures = scheduler.generateSeasonFixtures(testLeague, 2025);

        // Map: teamId -> opponentId -> count
        Map<String, Map<String, Integer>> matchups = new HashMap<>();
        for (Team team : testLeague.teams()) {
            matchups.put(team.id(), new HashMap<>());
        }

        for (Fixture fixture : fixtures) {
            for (Match match : fixture.matches()) {
                String homeId = match.getHomeTeam().id();
                String awayId = match.getAwayTeam().id();
                matchups.get(homeId).merge(awayId, 1, Integer::sum);
                matchups.get(awayId).merge(homeId, 1, Integer::sum);
            }
        }

        for (Team team : testLeague.teams()) {
            for (Team opponent : testLeague.teams()) {
                if (!team.id().equals(opponent.id())) {
                    int count = matchups.get(team.id()).getOrDefault(opponent.id(), 0);
                    assertEquals(2, count,
                            team.name() + " should play " + opponent.name() + " exactly twice");
                }
            }
        }
    }

    @Test
    @DisplayName("Should throw exception for league with less than 2 teams")
    void shouldThrowExceptionForTooFewTeams() {
        League smallLeague = createTestLeague(1);

        assertThrows(IllegalArgumentException.class,
                () -> scheduler.generateSeasonFixtures(smallLeague, 2025),
                "Should throw exception for league with less than 2 teams");
    }

    @Test
    @DisplayName("Should handle 4-team league correctly")
    void shouldHandleFourTeamLeague() {
        League fourTeamLeague = createTestLeague(4);
        List<Fixture> fixtures = scheduler.generateSeasonFixtures(fourTeamLeague, 2025);

        // 4 teams = 6 matchweeks (3 first half + 3 second half)
        assertEquals(6, fixtures.size(), "Should have 6 matchweeks for 4 teams");

        // Each matchweek should have 2 matches
        for (Fixture fixture : fixtures) {
            assertEquals(2, fixture.matches().size(),
                    "Each matchweek should have 2 matches for 4 teams");
        }
    }

    private League createTestLeague(int numTeams) {
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= numTeams; i++) {
            teams.add(createTestTeam("team-" + i, "Team " + i));
        }
        return new League("test-league", "Test League", "Test Country", "/logo.png", teams);
    }

    private Team createTestTeam(String id, String name) {
        List<Player> players = List.of(
                new Player("gk-" + id, "Goalkeeper", Position.GOALKEEPER, 1, 75),
                new Player("def-" + id, "Defender", Position.DEFENDER, 4, 75),
                new Player("mid-" + id, "Midfielder", Position.MIDFIELDER, 8, 75),
                new Player("fwd-" + id, "Forward", Position.FORWARD, 9, 75)
        );
        return new Team(id, name, name.substring(0, Math.min(3, name.length())).toUpperCase(),
                "/badge.png", new TeamStrength(75, 75, 75, 75), players);
    }
}

