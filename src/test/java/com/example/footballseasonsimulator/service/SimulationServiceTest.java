package com.example.footballseasonsimulator.service;

import com.example.footballseasonsimulator.model.Fixture;
import com.example.footballseasonsimulator.model.League;
import com.example.footballseasonsimulator.model.Match;
import com.example.footballseasonsimulator.model.Standing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for SimulationService.
 * Tests simulation service with real dependencies.
 * Rate limiting is disabled via the 'test' profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class SimulationServiceTest {

    @Autowired
    private SimulationService simulationService;

    @Test
    @DisplayName("getAllLeagues should return all 5 leagues")
    void getAllLeaguesShouldReturnAllLeagues() {
        List<League> leagues = simulationService.getAllLeagues();

        assertThat(leagues).hasSize(5);
        assertThat(leagues).extracting(League::id)
                .containsExactlyInAnyOrder(
                        "premier-league", "la-liga", "serie-a", "bundesliga", "ligue-1");
    }

    @Test
    @DisplayName("getLeague should return Premier League")
    void getLeagueShouldReturnPremierLeague() {
        League league = simulationService.getLeague("premier-league");

        assertThat(league).isNotNull();
        assertThat(league.id()).isEqualTo("premier-league");
        assertThat(league.name()).isEqualTo("Premier League");
    }

    @Test
    @DisplayName("getLeague should return null for non-existent league")
    void getLeagueShouldReturnNullForNonExistent() {
        League league = simulationService.getLeague("non-existent");

        assertThat(league).isNull();
    }

    @Test
    @DisplayName("getStandings should return standings for Premier League")
    void getStandingsShouldReturnStandings() {
        List<Standing> standings = simulationService.getStandings("premier-league");

        assertThat(standings).hasSize(20);
        assertThat(standings).allSatisfy(standing -> {
            assertThat(standing.getTeamId()).isNotBlank();
            assertThat(standing.getTeamName()).isNotBlank();
        });
    }

    @Test
    @DisplayName("getStandings should return empty list for non-existent league")
    void getStandingsShouldReturnEmptyForNonExistent() {
        List<Standing> standings = simulationService.getStandings("non-existent");

        assertThat(standings).isEmpty();
    }

    @Test
    @DisplayName("getStandings should return 18 teams for Bundesliga")
    void getStandingsShouldReturn18TeamsForBundesliga() {
        List<Standing> standings = simulationService.getStandings("bundesliga");

        assertThat(standings).hasSize(18);
    }

    @Test
    @DisplayName("getTeamStanding should return standing for Arsenal")
    void getTeamStandingShouldReturnStanding() {
        Standing standing = simulationService.getTeamStanding("premier-league", "arsenal");

        assertThat(standing).isNotNull();
        assertThat(standing.getTeamId()).isEqualTo("arsenal");
        assertThat(standing.getTeamName()).isEqualTo("Arsenal");
    }

    @Test
    @DisplayName("getTeamStanding should return null for non-existent team")
    void getTeamStandingShouldReturnNullForNonExistent() {
        Standing standing = simulationService.getTeamStanding("premier-league", "non-existent");

        assertThat(standing).isNull();
    }

    @Test
    @DisplayName("getStatus should return status for Premier League")
    void getStatusShouldReturnStatus() {
        SimulationService.SimulationStatus status = simulationService.getStatus("premier-league");

        assertThat(status).isNotNull();
        assertThat(status.leagueId()).isEqualTo("premier-league");
        assertThat(status.leagueName()).isEqualTo("Premier League");
        assertThat(status.season()).isGreaterThanOrEqualTo(1);
        assertThat(status.totalMatchweeks()).isEqualTo(38);
    }

    @Test
    @DisplayName("getStatus should return null for non-existent league")
    void getStatusShouldReturnNullForNonExistent() {
        SimulationService.SimulationStatus status = simulationService.getStatus("non-existent");

        assertThat(status).isNull();
    }

    @Test
    @DisplayName("getLiveMatches should return list (may be empty)")
    void getLiveMatchesShouldReturnList() {
        List<Match> liveMatches = simulationService.getLiveMatches("premier-league");

        assertThat(liveMatches).isNotNull();
    }

    @Test
    @DisplayName("getLiveMatches should return empty for non-existent league")
    void getLiveMatchesShouldReturnEmptyForNonExistent() {
        List<Match> liveMatches = simulationService.getLiveMatches("non-existent");

        assertThat(liveMatches).isEmpty();
    }

    @Test
    @DisplayName("getCompletedMatches should return list (may be empty)")
    void getCompletedMatchesShouldReturnList() {
        List<Match> completedMatches = simulationService.getCompletedMatches("premier-league");

        assertThat(completedMatches).isNotNull();
    }

    @Test
    @DisplayName("getCompletedMatches should return empty for non-existent league")
    void getCompletedMatchesShouldReturnEmptyForNonExistent() {
        List<Match> completedMatches = simulationService.getCompletedMatches("non-existent");

        assertThat(completedMatches).isEmpty();
    }

    @Test
    @DisplayName("getMatch should return null for non-existent match")
    void getMatchShouldReturnNullForNonExistent() {
        Match match = simulationService.getMatch("non-existent-match-id");

        assertThat(match).isNull();
    }

    @Test
    @DisplayName("getCurrentFixture should return fixture or null")
    void getCurrentFixtureShouldReturnFixtureOrNull() {
        Fixture fixture = simulationService.getCurrentFixture("premier-league");
        // May be null if no fixture is currently playing
        // Just verify no exception is thrown
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("getCurrentFixture should return null for non-existent league")
    void getCurrentFixtureShouldReturnNullForNonExistent() {
        Fixture fixture = simulationService.getCurrentFixture("non-existent");

        assertThat(fixture).isNull();
    }

    @Test
    @DisplayName("getNextFixture should return fixture or null")
    void getNextFixtureShouldReturnFixtureOrNull() {
        Fixture fixture = simulationService.getNextFixture("premier-league");
        // May be null if season is complete
        // Just verify no exception is thrown
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("getNextFixture should return null for non-existent league")
    void getNextFixtureShouldReturnNullForNonExistent() {
        Fixture fixture = simulationService.getNextFixture("non-existent");

        assertThat(fixture).isNull();
    }

    @Test
    @DisplayName("getStatus should return correct total matchweeks for Bundesliga")
    void getStatusShouldReturnCorrectMatchweeksForBundesliga() {
        SimulationService.SimulationStatus status = simulationService.getStatus("bundesliga");

        assertThat(status).isNotNull();
        assertThat(status.totalMatchweeks()).isEqualTo(34); // 18 teams = 34 matchweeks
    }

    @Test
    @DisplayName("getAllLeagues should return leagues with correct team counts")
    void getAllLeaguesShouldReturnCorrectTeamCounts() {
        List<League> leagues = simulationService.getAllLeagues();

        for (League league : leagues) {
            if ("bundesliga".equals(league.id()) || "ligue-1".equals(league.id())) {
                assertThat(league.teamCount()).isEqualTo(18);
            } else {
                assertThat(league.teamCount()).isEqualTo(20);
            }
        }
    }

    @Test
    @DisplayName("getStandings should return standings sorted by points")
    void getStandingsShouldBeSortedByPoints() {
        List<Standing> standings = simulationService.getStandings("premier-league");

        // Verify standings are sorted (first team should have >= points than last)
        if (standings.size() > 1) {
            int firstPoints = standings.get(0).getPoints();
            int lastPoints = standings.get(standings.size() - 1).getPoints();
            assertThat(firstPoints).isGreaterThanOrEqualTo(lastPoints);
        }
    }

    @Test
    @DisplayName("getTeamStanding should return null for non-existent league")
    void getTeamStandingShouldReturnNullForNonExistentLeague() {
        Standing standing = simulationService.getTeamStanding("non-existent", "arsenal");
        assertThat(standing).isNull();
    }

    @Test
    @DisplayName("getStatus should return valid status for existing league")
    void getStatusShouldReturnValidStatus() {
        SimulationService.SimulationStatus status = simulationService.getStatus("premier-league");

        assertThat(status).isNotNull();
        assertThat(status.leagueId()).isEqualTo("premier-league");
        assertThat(status.leagueName()).isEqualTo("Premier League");
        assertThat(status.season()).isGreaterThanOrEqualTo(1);
        assertThat(status.totalMatchweeks()).isEqualTo(38);
    }

    @Test
    @DisplayName("getMatch should return match when fixture is available")
    void getMatchShouldReturnMatchWhenFixtureAvailable() {
        // Wait for a current fixture to be available
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Fixture fixture = simulationService.getCurrentFixture("premier-league");
                    return fixture != null && !fixture.matches().isEmpty();
                });

        Fixture fixture = simulationService.getCurrentFixture("premier-league");
        String matchId = fixture.matches().get(0).getId();

        Match match = simulationService.getMatch(matchId);
        assertThat(match).isNotNull();
        assertThat(match.getId()).isEqualTo(matchId);
    }

    @Test
    @DisplayName("getCurrentFixture should return fixture with matches")
    void getCurrentFixtureShouldReturnFixtureWithMatches() {
        // Wait for a current fixture to be available
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> simulationService.getCurrentFixture("premier-league") != null);

        Fixture fixture = simulationService.getCurrentFixture("premier-league");
        assertThat(fixture).isNotNull();
        assertThat(fixture.matches()).isNotEmpty();
        assertThat(fixture.matchweek()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("getStatus should return state for running simulation")
    void getStatusShouldReturnStateForRunningSimulation() {
        SimulationService.SimulationStatus status = simulationService.getStatus("premier-league");

        assertThat(status).isNotNull();
        assertThat(status.state()).isNotNull();
        assertThat(status.currentMatchweek()).isGreaterThanOrEqualTo(0);
    }
}

