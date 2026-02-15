package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.Match;
import com.example.footballseasonsimulator.model.MatchEvent;
import com.example.footballseasonsimulator.model.Standing;
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

import java.util.List;

/**
 * REST controller for match-related endpoints.
 * Provides access to individual match details, events, and statistics.
 */
@RestController
@RequestMapping("/api/v1/matches")
@Validated
@Tag(name = "Matches", description = "Match details and events endpoints. Access individual match information, live events, and match statistics.")
public class MatchController {

    private final SimulationService simulationService;

    public MatchController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Operation(
        summary = "Get match details",
        description = "Returns detailed information about a specific match including teams, score, events, and statistics"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Match found",
            content = @Content(schema = @Schema(implementation = MatchDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Match not found", content = @Content)
    })
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailDTO> getMatch(
            @Parameter(description = "Unique match identifier (UUID format)")
            @PathVariable
            @NotBlank(message = "Match ID is required")
            @Pattern(regexp = "^[a-f0-9-]{36}$", message = "Match ID must be a valid UUID")
            String matchId) {
        Match match = simulationService.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }

        // Get form data for both teams
        Standing homeStanding = simulationService.getTeamStanding(match.getLeagueId(), match.getHomeTeam().id());
        Standing awayStanding = simulationService.getTeamStanding(match.getLeagueId(), match.getAwayTeam().id());
        String homeForm = homeStanding != null ? homeStanding.getFormString() : "";
        String awayForm = awayStanding != null ? awayStanding.getFormString() : "";

        return ResponseEntity.ok(MatchDetailDTO.from(match, homeForm, awayForm));
    }

    @Operation(
        summary = "Get all match events",
        description = "Returns all events that occurred during the match including shots, fouls, corners, and more"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Match not found", content = @Content)
    })
    @GetMapping("/{matchId}/events")
    public ResponseEntity<List<MatchEvent>> getMatchEvents(
            @Parameter(description = "Unique match identifier")
            @PathVariable
            @NotBlank(message = "Match ID is required")
            @Pattern(regexp = "^[a-f0-9-]{36}$", message = "Match ID must be a valid UUID")
            String matchId) {
        Match match = simulationService.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(match.getEvents());
    }

    @Operation(
        summary = "Get significant events only",
        description = "Returns only significant events: goals, cards, penalties, kick-off, half-time, and full-time"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Significant events retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Match not found", content = @Content)
    })
    @GetMapping("/{matchId}/events/significant")
    public ResponseEntity<List<MatchEvent>> getSignificantEvents(
            @Parameter(description = "Unique match identifier")
            @PathVariable
            @NotBlank(message = "Match ID is required")
            @Pattern(regexp = "^[a-f0-9-]{36}$", message = "Match ID must be a valid UUID")
            String matchId) {
        Match match = simulationService.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(match.getSignificantEvents());
    }
    
    // DTOs

    @Schema(description = "Detailed match information including teams, events, and statistics")
    public record MatchDetailDTO(
        @Schema(description = "Unique match identifier (UUID)")
        String id,
        @Schema(description = "League identifier", example = "premier-league")
        String leagueId,
        @Schema(description = "Season number", example = "1")
        int season,
        @Schema(description = "Matchweek number", example = "15")
        int matchweek,
        @Schema(description = "Home team information")
        TeamInfoDTO homeTeam,
        @Schema(description = "Away team information")
        TeamInfoDTO awayTeam,
        @Schema(description = "Home team score", example = "2")
        int homeScore,
        @Schema(description = "Away team score", example = "1")
        int awayScore,
        @Schema(description = "Match phase", example = "SECOND_HALF")
        String phase,
        @Schema(description = "Current match time display", example = "67'")
        String timeDisplay,
        @Schema(description = "List of significant match events (goals, cards, etc.)")
        List<EventDTO> events,
        @Schema(description = "Match statistics")
        MatchStatsDTO stats
    ) {
        public static MatchDetailDTO from(Match match, String homeForm, String awayForm) {
            List<EventDTO> events = match.getEvents().stream()
                .filter(e -> e.type().isSignificant())
                .map(EventDTO::from)
                .toList();

            return new MatchDetailDTO(
                match.getId(),
                match.getLeagueId(),
                match.getSeason(),
                match.getMatchweek(),
                TeamInfoDTO.from(match.getHomeTeam(), homeForm),
                TeamInfoDTO.from(match.getAwayTeam(), awayForm),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getPhase().name(),
                match.getTimeDisplay(),
                events,
                MatchStatsDTO.calculate(match)
            );
        }
    }

    @Schema(description = "Team information with recent form")
    public record TeamInfoDTO(
        @Schema(description = "Team identifier", example = "arsenal")
        String id,
        @Schema(description = "Full team name", example = "Arsenal FC")
        String name,
        @Schema(description = "Short team name (3 letters)", example = "ARS")
        String shortName,
        @Schema(description = "URL to team badge image")
        String badgeUrl,
        @Schema(description = "Recent form (last 5 matches: W=Win, D=Draw, L=Loss)", example = "WWDLW")
        String form
    ) {
        public static TeamInfoDTO from(com.example.footballseasonsimulator.model.Team team, String form) {
            return new TeamInfoDTO(team.id(), team.name(), team.shortName(), team.badgeUrl(), form);
        }
    }

    @Schema(description = "Match event information")
    public record EventDTO(
        @Schema(description = "Unique event identifier")
        String id,
        @Schema(description = "Event time display", example = "45+2'")
        String displayTime,
        @Schema(description = "Event type (GOAL, YELLOW_CARD, RED_CARD, etc.)", example = "GOAL")
        String type,
        @Schema(description = "Team identifier for the event", example = "arsenal")
        String teamId,
        @Schema(description = "Player name involved in the event", example = "Bukayo Saka")
        String playerName,
        @Schema(description = "Event description", example = "GOAL! Bukayo Saka scores!")
        String description
    ) {
        public static EventDTO from(MatchEvent event) {
            return new EventDTO(
                event.id(),
                event.getDisplayTime(),
                event.type().name(),
                event.teamId(),
                event.playerName(),
                event.description()
            );
        }
    }

    @Schema(description = "Match statistics for both teams")
    public record MatchStatsDTO(
        @Schema(description = "Home team total shots", example = "12")
        int homeShots,
        @Schema(description = "Away team total shots", example = "8")
        int awayShots,
        @Schema(description = "Home team shots on target", example = "5")
        int homeShotsOnTarget,
        @Schema(description = "Away team shots on target", example = "3")
        int awayShotsOnTarget,
        @Schema(description = "Home team corners", example = "6")
        int homeCorners,
        @Schema(description = "Away team corners", example = "4")
        int awayCorners,
        @Schema(description = "Home team fouls", example = "10")
        int homeFouls,
        @Schema(description = "Away team fouls", example = "12")
        int awayFouls,
        @Schema(description = "Home team yellow cards", example = "2")
        int homeYellowCards,
        @Schema(description = "Away team yellow cards", example = "1")
        int awayYellowCards,
        @Schema(description = "Home team red cards", example = "0")
        int homeRedCards,
        @Schema(description = "Away team red cards", example = "0")
        int awayRedCards
    ) {
        public static MatchStatsDTO calculate(Match match) {
            int homeShots = 0, awayShots = 0;
            int homeShotsOnTarget = 0, awayShotsOnTarget = 0;
            int homeCorners = 0, awayCorners = 0;
            int homeFouls = 0, awayFouls = 0;
            int homeYellowCards = 0, awayYellowCards = 0;
            int homeRedCards = 0, awayRedCards = 0;

            String homeId = match.getHomeTeam().id();

            for (MatchEvent event : match.getEvents()) {
                boolean isHome = homeId.equals(event.teamId());
                switch (event.type()) {
                    case SHOT_ON_TARGET, GOAL, PENALTY_SCORED -> {
                        if (isHome) { homeShots++; homeShotsOnTarget++; }
                        else { awayShots++; awayShotsOnTarget++; }
                    }
                    case SHOT_OFF_TARGET -> {
                        if (isHome) homeShots++; else awayShots++;
                    }
                    case CORNER_KICK -> {
                        if (isHome) homeCorners++; else awayCorners++;
                    }
                    case FOUL -> {
                        if (isHome) homeFouls++; else awayFouls++;
                    }
                    case YELLOW_CARD, SECOND_YELLOW -> {
                        if (isHome) homeYellowCards++; else awayYellowCards++;
                    }
                    case RED_CARD -> {
                        if (isHome) homeRedCards++; else awayRedCards++;
                    }
                    default -> {}
                }
            }

            return new MatchStatsDTO(
                homeShots, awayShots, homeShotsOnTarget, awayShotsOnTarget,
                homeCorners, awayCorners, homeFouls, awayFouls,
                homeYellowCards, awayYellowCards, homeRedCards, awayRedCards
            );
        }
    }
}

