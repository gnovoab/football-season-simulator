package com.example.footballseasonsimulator.service;

import com.example.footballseasonsimulator.model.League;
import com.example.footballseasonsimulator.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for LeagueDataService.
 * Tests actual JSON loading and data retrieval.
 */
@SpringBootTest
class LeagueDataServiceTest {

    @Autowired
    private LeagueDataService leagueDataService;

    @Test
    @DisplayName("getAllLeagues should return all 5 leagues")
    void getAllLeaguesShouldReturnAllLeagues() {
        List<League> leagues = leagueDataService.getAllLeagues();

        assertThat(leagues).hasSize(5);
        assertThat(leagues).extracting(League::id)
                .containsExactlyInAnyOrder(
                        "premier-league", "la-liga", "serie-a", "bundesliga", "ligue-1");
    }

    @Test
    @DisplayName("getLeague should return Premier League with 20 teams")
    void getLeagueShouldReturnPremierLeague() {
        League league = leagueDataService.getLeague("premier-league");

        assertThat(league).isNotNull();
        assertThat(league.id()).isEqualTo("premier-league");
        assertThat(league.name()).isEqualTo("Premier League");
        assertThat(league.country()).isEqualTo("England");
        assertThat(league.teamCount()).isEqualTo(20);
        assertThat(league.teams()).hasSize(20);
    }

    @Test
    @DisplayName("getLeague should return La Liga with 20 teams")
    void getLeagueShouldReturnLaLiga() {
        League league = leagueDataService.getLeague("la-liga");

        assertThat(league).isNotNull();
        assertThat(league.id()).isEqualTo("la-liga");
        assertThat(league.name()).isEqualTo("La Liga");
        assertThat(league.country()).isEqualTo("Spain");
        assertThat(league.teamCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("getLeague should return Serie A with 20 teams")
    void getLeagueShouldReturnSerieA() {
        League league = leagueDataService.getLeague("serie-a");

        assertThat(league).isNotNull();
        assertThat(league.id()).isEqualTo("serie-a");
        assertThat(league.name()).isEqualTo("Serie A");
        assertThat(league.country()).isEqualTo("Italy");
        assertThat(league.teamCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("getLeague should return Bundesliga with 18 teams")
    void getLeagueShouldReturnBundesliga() {
        League league = leagueDataService.getLeague("bundesliga");

        assertThat(league).isNotNull();
        assertThat(league.id()).isEqualTo("bundesliga");
        assertThat(league.name()).isEqualTo("Bundesliga");
        assertThat(league.country()).isEqualTo("Germany");
        assertThat(league.teamCount()).isEqualTo(18);
    }

    @Test
    @DisplayName("getLeague should return Ligue 1 with 18 teams")
    void getLeagueShouldReturnLigue1() {
        League league = leagueDataService.getLeague("ligue-1");

        assertThat(league).isNotNull();
        assertThat(league.id()).isEqualTo("ligue-1");
        assertThat(league.name()).isEqualTo("Ligue 1");
        assertThat(league.country()).isEqualTo("France");
        assertThat(league.teamCount()).isEqualTo(18);
    }

    @Test
    @DisplayName("getLeague should return null for non-existent league")
    void getLeagueShouldReturnNullForNonExistent() {
        League league = leagueDataService.getLeague("non-existent");

        assertThat(league).isNull();
    }

    @Test
    @DisplayName("getTeam should return Arsenal from Premier League")
    void getTeamShouldReturnArsenal() {
        Team team = leagueDataService.getTeam("premier-league", "arsenal");

        assertThat(team).isNotNull();
        assertThat(team.id()).isEqualTo("arsenal");
        assertThat(team.name()).isEqualTo("Arsenal");
        assertThat(team.shortName()).isEqualTo("ARS");
        assertThat(team.players()).isNotEmpty();
        assertThat(team.strength()).isNotNull();
    }

    @Test
    @DisplayName("getTeam should return Real Madrid from La Liga")
    void getTeamShouldReturnRealMadrid() {
        Team team = leagueDataService.getTeam("la-liga", "real-madrid");

        assertThat(team).isNotNull();
        assertThat(team.id()).isEqualTo("real-madrid");
        assertThat(team.name()).isEqualTo("Real Madrid");
    }

    @Test
    @DisplayName("getTeam should return null for non-existent team")
    void getTeamShouldReturnNullForNonExistent() {
        Team team = leagueDataService.getTeam("premier-league", "non-existent");

        assertThat(team).isNull();
    }

    @Test
    @DisplayName("getTeam should return null for non-existent league")
    void getTeamShouldReturnNullForNonExistentLeague() {
        Team team = leagueDataService.getTeam("non-existent", "arsenal");

        assertThat(team).isNull();
    }

    @Test
    @DisplayName("Teams should have valid strength values")
    void teamsShouldHaveValidStrength() {
        League league = leagueDataService.getLeague("premier-league");

        for (Team team : league.teams()) {
            assertThat(team.strength().attack()).isBetween(1, 100);
            assertThat(team.strength().midfield()).isBetween(1, 100);
            assertThat(team.strength().defense()).isBetween(1, 100);
            assertThat(team.strength().goalkeeper()).isBetween(1, 100);
        }
    }

    @Test
    @DisplayName("Teams should have players with valid positions")
    void teamsShouldHavePlayersWithValidPositions() {
        League league = leagueDataService.getLeague("premier-league");
        Team team = league.teams().get(0);

        assertThat(team.players()).isNotEmpty();
        assertThat(team.players()).allSatisfy(player -> {
            assertThat(player.position()).isNotNull();
            assertThat(player.name()).isNotBlank();
            assertThat(player.rating()).isBetween(1, 100);
        });
    }
}

