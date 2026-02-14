package com.example.footballseasonsimulator.websocket;

import com.example.footballseasonsimulator.model.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes match events and state updates via WebSocket.
 */
@Component
public class MatchEventPublisher {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public MatchEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Publish a match event to subscribers.
     */
    public void publishEvent(String leagueId, MatchEvent event) {
        // Publish to league-specific topic
        messagingTemplate.convertAndSend("/topic/league/" + leagueId + "/events", event);
        
        // Also publish to global events topic
        messagingTemplate.convertAndSend("/topic/events", event);
    }
    
    /**
     * Publish match state update.
     */
    public void publishMatchState(Match match) {
        MatchStateDTO dto = new MatchStateDTO(match);
        
        // Publish to match-specific topic
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), dto);
        
        // Publish to league topic
        messagingTemplate.convertAndSend("/topic/league/" + match.getLeagueId() + "/matches", dto);
    }
    
    /**
     * Publish standings update.
     */
    public void publishStandings(String leagueId, int season, List<Standing> standings) {
        StandingsDTO dto = new StandingsDTO(leagueId, season, standings);
        messagingTemplate.convertAndSend("/topic/league/" + leagueId + "/standings", dto);
    }
    
    /**
     * Publish fixture start.
     */
    public void publishFixtureStart(Fixture fixture) {
        messagingTemplate.convertAndSend("/topic/league/" + fixture.leagueId() + "/fixture", fixture);
    }
    
    /**
     * Publish season state change.
     */
    public void publishSeasonState(String leagueId, SeasonState state, int season, int matchweek) {
        SeasonStateDTO dto = new SeasonStateDTO(leagueId, state, season, matchweek);
        messagingTemplate.convertAndSend("/topic/league/" + leagueId + "/state", dto);
    }

    /**
     * Publish countdown update before matchweek starts.
     */
    public void publishCountdown(String leagueId, int matchweek, int secondsRemaining, Fixture upcomingFixture) {
        CountdownDTO dto = new CountdownDTO(leagueId, matchweek, secondsRemaining, upcomingFixture);
        messagingTemplate.convertAndSend("/topic/league/" + leagueId + "/countdown", dto);
    }

    // DTOs for WebSocket messages
    
    public record MatchStateDTO(
        String matchId,
        String leagueId,
        String homeTeamId,
        String homeTeamName,
        String homeTeamBadge,
        String awayTeamId,
        String awayTeamName,
        String awayTeamBadge,
        int homeScore,
        int awayScore,
        String phase,
        String timeDisplay,
        int matchweek
    ) {
        public MatchStateDTO(Match match) {
            this(
                match.getId(),
                match.getLeagueId(),
                match.getHomeTeam().id(),
                match.getHomeTeam().name(),
                match.getHomeTeam().badgeUrl(),
                match.getAwayTeam().id(),
                match.getAwayTeam().name(),
                match.getAwayTeam().badgeUrl(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getPhase().name(),
                match.getTimeDisplay(),
                match.getMatchweek()
            );
        }
    }
    
    public record StandingsDTO(
        String leagueId,
        int season,
        List<Standing> standings
    ) {}
    
    public record SeasonStateDTO(
        String leagueId,
        SeasonState state,
        int season,
        int matchweek
    ) {}

    public record CountdownDTO(
        String leagueId,
        int matchweek,
        int secondsRemaining,
        Fixture upcomingFixture
    ) {}
}

