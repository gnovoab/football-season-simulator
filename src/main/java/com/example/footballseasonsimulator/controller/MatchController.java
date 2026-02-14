package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.Match;
import com.example.footballseasonsimulator.model.MatchEvent;
import com.example.footballseasonsimulator.model.Standing;
import com.example.footballseasonsimulator.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for match-related endpoints.
 */
@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*")
public class MatchController {
    
    private final SimulationService simulationService;
    
    public MatchController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }
    
    /**
     * Get match details by ID.
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailDTO> getMatch(@PathVariable String matchId) {
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
    
    /**
     * Get events for a match.
     */
    @GetMapping("/{matchId}/events")
    public ResponseEntity<List<MatchEvent>> getMatchEvents(@PathVariable String matchId) {
        Match match = simulationService.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(match.getEvents());
    }
    
    /**
     * Get significant events only (goals, cards, etc.)
     */
    @GetMapping("/{matchId}/events/significant")
    public ResponseEntity<List<MatchEvent>> getSignificantEvents(@PathVariable String matchId) {
        Match match = simulationService.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(match.getSignificantEvents());
    }
    
    // DTOs
    
    public record MatchDetailDTO(
        String id,
        String leagueId,
        int season,
        int matchweek,
        TeamInfoDTO homeTeam,
        TeamInfoDTO awayTeam,
        int homeScore,
        int awayScore,
        String phase,
        String timeDisplay,
        List<EventDTO> events,
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

    public record TeamInfoDTO(
        String id,
        String name,
        String shortName,
        String badgeUrl,
        String form
    ) {
        public static TeamInfoDTO from(com.example.footballseasonsimulator.model.Team team, String form) {
            return new TeamInfoDTO(team.id(), team.name(), team.shortName(), team.badgeUrl(), form);
        }
    }
    
    public record EventDTO(
        String id,
        String displayTime,
        String type,
        String teamId,
        String playerName,
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
    
    public record MatchStatsDTO(
        int homeShots,
        int awayShots,
        int homeShotsOnTarget,
        int awayShotsOnTarget,
        int homeCorners,
        int awayCorners,
        int homeFouls,
        int awayFouls,
        int homeYellowCards,
        int awayYellowCards,
        int homeRedCards,
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

