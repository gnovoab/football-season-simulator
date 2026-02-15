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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for statistics-related endpoints.
 * Provides access to player and team statistics across leagues.
 */
@RestController
@RequestMapping("/api/v1/statistics")
@Validated
@Tag(name = "Statistics", description = "Player and team statistics endpoints. Access top scorers, team performance, and league statistics.")
public class StatisticsController {

    private final SimulationService simulationService;

    public StatisticsController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Operation(
        summary = "Get top scorers",
        description = "Returns the top goal scorers in a league, sorted by goals scored"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved top scorers")
    @GetMapping("/{leagueId}/top-scorers")
    public List<PlayerStatsDTO> getTopScorers(
            @Parameter(description = "League ID")
            @PathVariable
            @NotBlank(message = "League ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "League ID must contain only lowercase letters, numbers, and hyphens")
            String leagueId,
            @Parameter(description = "Maximum number of players to return")
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, PlayerStatsDTO> playerStats = new HashMap<>();
        
        for (Match match : simulationService.getCompletedMatches(leagueId)) {
            for (MatchEvent event : match.getEvents()) {
                if (event.type().isGoal() && event.playerId() != null) {
                    String playerId = event.playerId();
                    String teamId = event.teamId();
                    String teamName = teamId.equals(match.getHomeTeam().id()) 
                        ? match.getHomeTeam().name() 
                        : match.getAwayTeam().name();
                    
                    playerStats.compute(playerId, (k, v) -> {
                        if (v == null) {
                            return new PlayerStatsDTO(playerId, event.playerName(), teamId, teamName, 1, 0, 0, 0);
                        }
                        return new PlayerStatsDTO(v.playerId(), v.playerName(), v.teamId(), v.teamName(),
                                v.goals() + 1, v.yellowCards(), v.redCards(), v.appearances());
                    });
                }
            }
        }
        
        return playerStats.values().stream()
                .sorted(Comparator.comparingInt(PlayerStatsDTO::goals).reversed())
                .limit(limit)
                .toList();
    }

    @Operation(
        summary = "Get team statistics",
        description = "Returns detailed statistics for a specific team"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team statistics found"),
        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    })
    @GetMapping("/{leagueId}/teams/{teamId}")
    public ResponseEntity<TeamStatsDTO> getTeamStats(
            @Parameter(description = "League ID")
            @PathVariable
            @NotBlank(message = "League ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "League ID must contain only lowercase letters, numbers, and hyphens")
            String leagueId,
            @Parameter(description = "Team ID")
            @PathVariable
            @NotBlank(message = "Team ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "Team ID must contain only lowercase letters, numbers, and hyphens")
            String teamId) {
        
        Standing standing = simulationService.getTeamStanding(leagueId, teamId);
        if (standing == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Calculate additional stats from matches
        int totalShots = 0, shotsOnTarget = 0, corners = 0, fouls = 0, yellowCards = 0, redCards = 0;
        
        for (Match match : simulationService.getCompletedMatches(leagueId)) {
            if (!match.getHomeTeam().id().equals(teamId) && !match.getAwayTeam().id().equals(teamId)) {
                continue;
            }
            
            for (MatchEvent event : match.getEvents()) {
                if (!teamId.equals(event.teamId())) continue;
                
                switch (event.type()) {
                    case SHOT_ON_TARGET, GOAL, PENALTY_SCORED -> { totalShots++; shotsOnTarget++; }
                    case SHOT_OFF_TARGET -> totalShots++;
                    case CORNER_KICK -> corners++;
                    case FOUL -> fouls++;
                    case YELLOW_CARD, SECOND_YELLOW -> yellowCards++;
                    case RED_CARD -> redCards++;
                    default -> {}
                }
            }
        }
        
        return ResponseEntity.ok(new TeamStatsDTO(
                standing.getTeamId(),
                standing.getTeamName(),
                standing.getTeamBadgeUrl(),
                standing.getPlayed(),
                standing.getWon(),
                standing.getDrawn(),
                standing.getLost(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDifference(),
                standing.getPoints(),
                standing.getFormString(),
                totalShots,
                shotsOnTarget,
                corners,
                fouls,
                yellowCards,
                redCards
        ));
    }

    @Operation(
        summary = "Get league statistics summary",
        description = "Returns aggregate statistics for the entire league"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved league statistics")
    @GetMapping("/{leagueId}/summary")
    public LeagueStatsDTO getLeagueStats(
            @Parameter(description = "League ID")
            @PathVariable
            @NotBlank(message = "League ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "League ID must contain only lowercase letters, numbers, and hyphens")
            String leagueId) {

        List<Match> completedMatches = simulationService.getCompletedMatches(leagueId);

        int totalGoals = 0, totalMatches = completedMatches.size();
        int homeWins = 0, awayWins = 0, draws = 0;
        int totalYellowCards = 0, totalRedCards = 0;
        int highestScore = 0;
        String highestScoringMatch = "";

        for (Match match : completedMatches) {
            int matchGoals = match.getHomeScore() + match.getAwayScore();
            totalGoals += matchGoals;

            if (match.getHomeScore() > match.getAwayScore()) homeWins++;
            else if (match.getAwayScore() > match.getHomeScore()) awayWins++;
            else draws++;

            if (matchGoals > highestScore) {
                highestScore = matchGoals;
                highestScoringMatch = match.getHomeTeam().shortName() + " " +
                    match.getHomeScore() + "-" + match.getAwayScore() + " " +
                    match.getAwayTeam().shortName();
            }

            for (MatchEvent event : match.getEvents()) {
                if (event.type() == FootballEventType.YELLOW_CARD ||
                    event.type() == FootballEventType.SECOND_YELLOW) {
                    totalYellowCards++;
                } else if (event.type() == FootballEventType.RED_CARD) {
                    totalRedCards++;
                }
            }
        }

        double avgGoalsPerMatch = totalMatches > 0 ? (double) totalGoals / totalMatches : 0;

        return new LeagueStatsDTO(
                leagueId,
                totalMatches,
                totalGoals,
                Math.round(avgGoalsPerMatch * 100.0) / 100.0,
                homeWins,
                awayWins,
                draws,
                totalYellowCards,
                totalRedCards,
                highestScoringMatch
        );
    }

    // DTOs

    @Schema(description = "Player statistics")
    public record PlayerStatsDTO(
        @Schema(description = "Player ID") String playerId,
        @Schema(description = "Player name") String playerName,
        @Schema(description = "Team ID") String teamId,
        @Schema(description = "Team name") String teamName,
        @Schema(description = "Goals scored") int goals,
        @Schema(description = "Yellow cards received") int yellowCards,
        @Schema(description = "Red cards received") int redCards,
        @Schema(description = "Match appearances") int appearances
    ) {}

    @Schema(description = "Team statistics")
    public record TeamStatsDTO(
        @Schema(description = "Team ID") String teamId,
        @Schema(description = "Team name") String teamName,
        @Schema(description = "Team badge URL") String badgeUrl,
        @Schema(description = "Matches played") int played,
        @Schema(description = "Matches won") int won,
        @Schema(description = "Matches drawn") int drawn,
        @Schema(description = "Matches lost") int lost,
        @Schema(description = "Goals scored") int goalsFor,
        @Schema(description = "Goals conceded") int goalsAgainst,
        @Schema(description = "Goal difference") int goalDifference,
        @Schema(description = "Total points") int points,
        @Schema(description = "Recent form (last 5 matches)") String form,
        @Schema(description = "Total shots") int totalShots,
        @Schema(description = "Shots on target") int shotsOnTarget,
        @Schema(description = "Corners won") int corners,
        @Schema(description = "Fouls committed") int fouls,
        @Schema(description = "Yellow cards") int yellowCards,
        @Schema(description = "Red cards") int redCards
    ) {}

    @Schema(description = "League statistics summary")
    public record LeagueStatsDTO(
        @Schema(description = "League ID") String leagueId,
        @Schema(description = "Total matches played") int totalMatches,
        @Schema(description = "Total goals scored") int totalGoals,
        @Schema(description = "Average goals per match") double avgGoalsPerMatch,
        @Schema(description = "Home wins") int homeWins,
        @Schema(description = "Away wins") int awayWins,
        @Schema(description = "Draws") int draws,
        @Schema(description = "Total yellow cards") int totalYellowCards,
        @Schema(description = "Total red cards") int totalRedCards,
        @Schema(description = "Highest scoring match") String highestScoringMatch
    ) {}
}

