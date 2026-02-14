package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.*;
import com.example.footballseasonsimulator.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for league-related endpoints.
 * Provides access to league information, standings, fixtures, and simulation status.
 */
@RestController
@RequestMapping("/api/v1/leagues")
@CrossOrigin(origins = "*")
@Tag(name = "Leagues", description = "League management and information endpoints. Access league data, standings, fixtures, and simulation status for all 5 European leagues.")
public class LeagueController {

    private final SimulationService simulationService;

    public LeagueController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Operation(
        summary = "Get all leagues",
        description = "Returns a list of all available leagues (Premier League, La Liga, Serie A, Bundesliga, Ligue 1)"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all leagues")
    @GetMapping
    public List<LeagueDTO> getAllLeagues() {
        return simulationService.getAllLeagues().stream()
            .map(LeagueDTO::from)
            .toList();
    }

    @Operation(
        summary = "Get league by ID",
        description = "Returns detailed information about a specific league"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "League found"),
        @ApiResponse(responseCode = "404", description = "League not found", content = @Content)
    })
    @GetMapping("/{leagueId}")
    public ResponseEntity<LeagueDTO> getLeague(
            @Parameter(description = "League ID (e.g., premier-league, la-liga, serie-a, bundesliga, ligue-1)")
            @PathVariable String leagueId) {
        League league = simulationService.getLeague(leagueId);
        if (league == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(LeagueDTO.from(league));
    }

    @Operation(
        summary = "Get league standings",
        description = "Returns the current standings table for a league, sorted by points, goal difference, and goals scored"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved standings")
    @GetMapping("/{leagueId}/standings")
    public List<Standing> getStandings(
            @Parameter(description = "League ID") @PathVariable String leagueId) {
        return simulationService.getStandings(leagueId);
    }

    @Operation(
        summary = "Get current fixture",
        description = "Returns the current matchweek fixture with all matches being played"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current fixture found"),
        @ApiResponse(responseCode = "204", description = "No fixture currently in progress", content = @Content)
    })
    @GetMapping("/{leagueId}/fixture")
    public ResponseEntity<FixtureDTO> getCurrentFixture(
            @Parameter(description = "League ID") @PathVariable String leagueId) {
        Fixture fixture = simulationService.getCurrentFixture(leagueId);
        if (fixture == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(FixtureDTO.from(fixture));
    }

    @Operation(
        summary = "Get live matches",
        description = "Returns all matches currently being played in the league"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved live matches")
    @GetMapping("/{leagueId}/live")
    public List<MatchDTO> getLiveMatches(
            @Parameter(description = "League ID") @PathVariable String leagueId) {
        return simulationService.getLiveMatches(leagueId).stream()
            .map(MatchDTO::from)
            .toList();
    }

    @Operation(
        summary = "Get completed matches",
        description = "Returns all completed matches in the current season"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved completed matches")
    @GetMapping("/{leagueId}/results")
    public List<MatchDTO> getCompletedMatches(
            @Parameter(description = "League ID") @PathVariable String leagueId) {
        return simulationService.getCompletedMatches(leagueId).stream()
            .map(MatchDTO::from)
            .toList();
    }

    @Operation(
        summary = "Get simulation status",
        description = "Returns the current simulation status including season number, matchweek, and state"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "League not found", content = @Content)
    })
    @GetMapping("/{leagueId}/status")
    public ResponseEntity<SimulationService.SimulationStatus> getStatus(
            @Parameter(description = "League ID") @PathVariable String leagueId) {
        SimulationService.SimulationStatus status = simulationService.getStatus(leagueId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "Get next fixture",
        description = "Returns the upcoming matchweek fixture"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Next fixture found"),
        @ApiResponse(responseCode = "204", description = "No upcoming fixture (season complete)", content = @Content)
    })
    @GetMapping("/{leagueId}/next-fixture")
    public ResponseEntity<FixtureDTO> getNextFixture(
            @Parameter(description = "League ID") @PathVariable String leagueId) {
        Fixture fixture = simulationService.getNextFixture(leagueId);
        if (fixture == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(FixtureDTO.from(fixture));
    }

    // DTOs

    @Schema(description = "League information")
    public record LeagueDTO(
        @Schema(description = "Unique league identifier", example = "premier-league")
        String id,
        @Schema(description = "Full league name", example = "Premier League")
        String name,
        @Schema(description = "Country of the league", example = "England")
        String country,
        @Schema(description = "URL to league logo image")
        String logoUrl,
        @Schema(description = "Number of teams in the league", example = "20")
        int teamCount
    ) {
        public static LeagueDTO from(League league) {
            return new LeagueDTO(
                league.id(),
                league.name(),
                league.country(),
                league.logoUrl(),
                league.teamCount()
            );
        }
    }

    @Schema(description = "Matchweek fixture containing all matches")
    public record FixtureDTO(
        @Schema(description = "League identifier", example = "premier-league")
        String leagueId,
        @Schema(description = "Season number", example = "1")
        int season,
        @Schema(description = "Matchweek number (1-38 for 20-team leagues)", example = "15")
        int matchweek,
        @Schema(description = "List of matches in this fixture")
        List<MatchDTO> matches
    ) {
        public static FixtureDTO from(Fixture fixture) {
            return new FixtureDTO(
                fixture.leagueId(),
                fixture.season(),
                fixture.matchweek(),
                fixture.matches().stream().map(MatchDTO::from).toList()
            );
        }
    }

    @Schema(description = "Match summary information")
    public record MatchDTO(
        @Schema(description = "Unique match identifier (UUID)")
        String id,
        @Schema(description = "Home team identifier", example = "arsenal")
        String homeTeamId,
        @Schema(description = "Home team full name", example = "Arsenal FC")
        String homeTeamName,
        @Schema(description = "Home team short name (3 letters)", example = "ARS")
        String homeTeamShortName,
        @Schema(description = "URL to home team badge")
        String homeTeamBadge,
        @Schema(description = "Away team identifier", example = "chelsea")
        String awayTeamId,
        @Schema(description = "Away team full name", example = "Chelsea FC")
        String awayTeamName,
        @Schema(description = "Away team short name (3 letters)", example = "CHE")
        String awayTeamShortName,
        @Schema(description = "URL to away team badge")
        String awayTeamBadge,
        @Schema(description = "Home team score", example = "2")
        int homeScore,
        @Schema(description = "Away team score", example = "1")
        int awayScore,
        @Schema(description = "Match phase: NOT_STARTED, FIRST_HALF, HALF_TIME, SECOND_HALF, FULL_TIME", example = "SECOND_HALF")
        String phase,
        @Schema(description = "Current match time display", example = "67'")
        String timeDisplay,
        @Schema(description = "Matchweek number", example = "15")
        int matchweek
    ) {
        public static MatchDTO from(Match match) {
            return new MatchDTO(
                match.getId(),
                match.getHomeTeam().id(),
                match.getHomeTeam().name(),
                match.getHomeTeam().shortName(),
                match.getHomeTeam().badgeUrl(),
                match.getAwayTeam().id(),
                match.getAwayTeam().name(),
                match.getAwayTeam().shortName(),
                match.getAwayTeam().badgeUrl(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getPhase().name(),
                match.getTimeDisplay(),
                match.getMatchweek()
            );
        }
    }
}

