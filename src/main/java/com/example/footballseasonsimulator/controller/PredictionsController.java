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

/**
 * REST controller for match prediction endpoints.
 * Provides win probabilities, expected goals, and other match predictions.
 */
@RestController
@RequestMapping("/api/v1/predictions")
@Validated
@Tag(name = "Predictions", description = "Match prediction endpoints. Get win probabilities, expected goals, and other match predictions.")
public class PredictionsController {

    private static final double HOME_ADVANTAGE = 1.08;
    private static final double BASE_HOME_WIN = 45.0;
    private static final double BASE_AWAY_WIN = 30.0;
    private static final double BASE_DRAW = 25.0;
    private static final double BASE_HOME_GOALS = 1.5;
    private static final double BASE_AWAY_GOALS = 1.2;
    private static final int REFERENCE_STRENGTH = 85;

    private final SimulationService simulationService;

    public PredictionsController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Operation(
        summary = "Get match prediction",
        description = "Returns win probabilities, expected goals, and other predictions for a specific match"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prediction calculated successfully"),
        @ApiResponse(responseCode = "404", description = "Match not found", content = @Content)
    })
    @GetMapping("/matches/{matchId}")
    public ResponseEntity<MatchPredictionDTO> getMatchPrediction(
            @Parameter(description = "Unique match identifier (UUID format)")
            @PathVariable
            @NotBlank(message = "Match ID is required")
            @Pattern(regexp = "^[a-f0-9-]{36}$", message = "Match ID must be a valid UUID")
            String matchId) {
        
        Match match = simulationService.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(calculatePrediction(match));
    }

    @Operation(
        summary = "Get head-to-head prediction",
        description = "Returns predictions for a hypothetical match between two teams"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prediction calculated successfully"),
        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    })
    @GetMapping("/head-to-head")
    public ResponseEntity<MatchPredictionDTO> getHeadToHeadPrediction(
            @Parameter(description = "League ID")
            @RequestParam
            @NotBlank(message = "League ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "League ID must contain only lowercase letters, numbers, and hyphens")
            String leagueId,
            @Parameter(description = "Home team ID")
            @RequestParam
            @NotBlank(message = "Home team ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "Team ID must contain only lowercase letters, numbers, and hyphens")
            String homeTeamId,
            @Parameter(description = "Away team ID")
            @RequestParam
            @NotBlank(message = "Away team ID is required")
            @Pattern(regexp = "^[a-z0-9-]+$", message = "Team ID must contain only lowercase letters, numbers, and hyphens")
            String awayTeamId) {
        
        League league = simulationService.getLeague(leagueId);
        if (league == null) {
            return ResponseEntity.notFound().build();
        }
        
        Team homeTeam = league.teams().stream()
                .filter(t -> t.id().equals(homeTeamId))
                .findFirst()
                .orElse(null);
        Team awayTeam = league.teams().stream()
                .filter(t -> t.id().equals(awayTeamId))
                .findFirst()
                .orElse(null);
        
        if (homeTeam == null || awayTeam == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Create a hypothetical match for prediction
        Match hypotheticalMatch = new Match(leagueId, 0, 0, homeTeam, awayTeam);
        return ResponseEntity.ok(calculatePrediction(hypotheticalMatch));
    }

    private MatchPredictionDTO calculatePrediction(Match match) {
        TeamStrength homeStr = match.getHomeTeam().strength();
        TeamStrength awayStr = match.getAwayTeam().strength();
        
        // Calculate overall strength
        double homeOverall = calculateOverall(homeStr) * HOME_ADVANTAGE;
        double awayOverall = calculateOverall(awayStr);
        double strDiff = homeOverall - awayOverall;
        
        // Win probabilities using Elo-style calculation
        double homeWin = BASE_HOME_WIN + strDiff * 1.5;
        double awayWin = BASE_AWAY_WIN - strDiff * 1.2;
        double evenness = 100 - Math.abs(strDiff) * 2;
        double draw = 20 + (evenness / 100) * 10;
        
        // Normalize to 100%
        double total = homeWin + draw + awayWin;
        homeWin = Math.round((homeWin / total) * 100);
        awayWin = Math.round((awayWin / total) * 100);
        draw = 100 - homeWin - awayWin;
        
        // Expected goals
        double homeGoalFactor = (homeStr.attack() / (double) REFERENCE_STRENGTH) 
                * (REFERENCE_STRENGTH / (double) awayStr.defense()) 
                * (homeStr.midfield() / (double) REFERENCE_STRENGTH);
        double awayGoalFactor = (awayStr.attack() / (double) REFERENCE_STRENGTH) 
                * (REFERENCE_STRENGTH / (double) homeStr.defense()) 
                * (awayStr.midfield() / (double) REFERENCE_STRENGTH);
        
        double homeExpectedGoals = BASE_HOME_GOALS * homeGoalFactor;
        double awayExpectedGoals = BASE_AWAY_GOALS * awayGoalFactor;
        double totalExpectedGoals = homeExpectedGoals + awayExpectedGoals;
        
        // Additional predictions
        int homeCorners = (int) Math.round(5 + (homeStr.attack() - 75) / 10.0 + (85 - awayStr.defense()) / 15.0);
        int awayCorners = (int) Math.round(5 + (awayStr.attack() - 75) / 10.0 + (85 - homeStr.defense()) / 15.0);
        
        int btts = clamp((int) Math.round(50 + (homeStr.attack() + awayStr.attack() - homeStr.defense() - awayStr.defense()) / 8.0), 0, 100);
        int over25 = clamp((int) Math.round(40 + totalExpectedGoals * 12), 0, 100);
        int over35 = clamp((int) Math.round(20 + totalExpectedGoals * 8), 0, 100);
        int cleanSheetHome = clamp((int) Math.round(30 + (homeStr.defense() - 80) * 2 - (awayStr.attack() - 80) * 1.5), 0, 100);
        int cleanSheetAway = clamp((int) Math.round(25 + (awayStr.defense() - 80) * 2 - (homeStr.attack() - 80) * 1.5), 0, 100);

        // Deterministic seed for red card/penalty based on team names
        int teamSeed = (match.getHomeTeam().name().length() + match.getAwayTeam().name().length()) % 10;
        int redCard = clamp((int) Math.round(6 + teamSeed * 0.8), 0, 100);
        int penalty = clamp((int) Math.round(12 + teamSeed * 0.6), 0, 100);

        return new MatchPredictionDTO(
                match.getId(),
                match.getHomeTeam().id(),
                match.getHomeTeam().name(),
                match.getAwayTeam().id(),
                match.getAwayTeam().name(),
                new WinProbabilityDTO((int) homeWin, (int) draw, (int) awayWin),
                new ExpectedGoalsDTO(
                        Math.round(homeExpectedGoals * 100.0) / 100.0,
                        Math.round(awayExpectedGoals * 100.0) / 100.0,
                        (int) Math.round(homeExpectedGoals),
                        (int) Math.round(awayExpectedGoals)
                ),
                new CornersDTO(homeCorners, awayCorners, homeCorners + awayCorners),
                new EventLikelihoodDTO(btts, over25, over35, cleanSheetHome, cleanSheetAway, redCard, penalty)
        );
    }

    private double calculateOverall(TeamStrength str) {
        return str.attack() * 0.3 + str.midfield() * 0.25 + str.defense() * 0.25 + str.goalkeeper() * 0.2;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // DTOs

    @Schema(description = "Complete match prediction")
    public record MatchPredictionDTO(
        @Schema(description = "Match ID") String matchId,
        @Schema(description = "Home team ID") String homeTeamId,
        @Schema(description = "Home team name") String homeTeamName,
        @Schema(description = "Away team ID") String awayTeamId,
        @Schema(description = "Away team name") String awayTeamName,
        @Schema(description = "Win probability breakdown") WinProbabilityDTO winProbability,
        @Schema(description = "Expected goals") ExpectedGoalsDTO expectedGoals,
        @Schema(description = "Corner predictions") CornersDTO corners,
        @Schema(description = "Event likelihood percentages") EventLikelihoodDTO eventLikelihood
    ) {}

    @Schema(description = "Win probability breakdown")
    public record WinProbabilityDTO(
        @Schema(description = "Home win probability %", example = "45") int homeWin,
        @Schema(description = "Draw probability %", example = "25") int draw,
        @Schema(description = "Away win probability %", example = "30") int awayWin
    ) {}

    @Schema(description = "Expected goals prediction")
    public record ExpectedGoalsDTO(
        @Schema(description = "Home team expected goals (xG)", example = "1.65") double homeXG,
        @Schema(description = "Away team expected goals (xG)", example = "1.23") double awayXG,
        @Schema(description = "Predicted home goals", example = "2") int predictedHomeGoals,
        @Schema(description = "Predicted away goals", example = "1") int predictedAwayGoals
    ) {}

    @Schema(description = "Corner predictions")
    public record CornersDTO(
        @Schema(description = "Predicted home corners", example = "6") int homeCorners,
        @Schema(description = "Predicted away corners", example = "5") int awayCorners,
        @Schema(description = "Predicted total corners", example = "11") int totalCorners
    ) {}

    @Schema(description = "Event likelihood percentages")
    public record EventLikelihoodDTO(
        @Schema(description = "Both teams to score %", example = "55") int btts,
        @Schema(description = "Over 2.5 goals %", example = "60") int over25Goals,
        @Schema(description = "Over 3.5 goals %", example = "35") int over35Goals,
        @Schema(description = "Home clean sheet %", example = "28") int homeCleanSheet,
        @Schema(description = "Away clean sheet %", example = "22") int awayCleanSheet,
        @Schema(description = "Red card in match %", example = "8") int redCard,
        @Schema(description = "Penalty in match %", example = "15") int penalty
    ) {}
}

