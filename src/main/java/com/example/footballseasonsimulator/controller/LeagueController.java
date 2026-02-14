package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.*;
import com.example.footballseasonsimulator.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for league-related endpoints.
 */
@RestController
@RequestMapping("/api/leagues")
@CrossOrigin(origins = "*")
public class LeagueController {
    
    private final SimulationService simulationService;
    
    public LeagueController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }
    
    /**
     * Get all leagues.
     */
    @GetMapping
    public List<LeagueDTO> getAllLeagues() {
        return simulationService.getAllLeagues().stream()
            .map(LeagueDTO::from)
            .toList();
    }
    
    /**
     * Get a specific league.
     */
    @GetMapping("/{leagueId}")
    public ResponseEntity<LeagueDTO> getLeague(@PathVariable String leagueId) {
        League league = simulationService.getLeague(leagueId);
        if (league == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(LeagueDTO.from(league));
    }
    
    /**
     * Get standings for a league.
     */
    @GetMapping("/{leagueId}/standings")
    public List<Standing> getStandings(@PathVariable String leagueId) {
        return simulationService.getStandings(leagueId);
    }
    
    /**
     * Get current fixture for a league.
     */
    @GetMapping("/{leagueId}/fixture")
    public ResponseEntity<FixtureDTO> getCurrentFixture(@PathVariable String leagueId) {
        Fixture fixture = simulationService.getCurrentFixture(leagueId);
        if (fixture == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(FixtureDTO.from(fixture));
    }
    
    /**
     * Get live matches for a league.
     */
    @GetMapping("/{leagueId}/live")
    public List<MatchDTO> getLiveMatches(@PathVariable String leagueId) {
        return simulationService.getLiveMatches(leagueId).stream()
            .map(MatchDTO::from)
            .toList();
    }
    
    /**
     * Get completed matches for a league.
     */
    @GetMapping("/{leagueId}/results")
    public List<MatchDTO> getCompletedMatches(@PathVariable String leagueId) {
        return simulationService.getCompletedMatches(leagueId).stream()
            .map(MatchDTO::from)
            .toList();
    }
    
    /**
     * Get simulation status for a league.
     */
    @GetMapping("/{leagueId}/status")
    public ResponseEntity<SimulationService.SimulationStatus> getStatus(@PathVariable String leagueId) {
        SimulationService.SimulationStatus status = simulationService.getStatus(leagueId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * Get next fixture for a league.
     */
    @GetMapping("/{leagueId}/next-fixture")
    public ResponseEntity<FixtureDTO> getNextFixture(@PathVariable String leagueId) {
        Fixture fixture = simulationService.getNextFixture(leagueId);
        if (fixture == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(FixtureDTO.from(fixture));
    }

    // DTOs
    
    public record LeagueDTO(
        String id,
        String name,
        String country,
        String logoUrl,
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
    
    public record FixtureDTO(
        String leagueId,
        int season,
        int matchweek,
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
    
    public record MatchDTO(
        String id,
        String homeTeamId,
        String homeTeamName,
        String homeTeamShortName,
        String homeTeamBadge,
        String awayTeamId,
        String awayTeamName,
        String awayTeamShortName,
        String awayTeamBadge,
        int homeScore,
        int awayScore,
        String phase,
        String timeDisplay,
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

