package com.example.footballseasonsimulator.service;

import com.example.footballseasonsimulator.model.League;
import com.example.footballseasonsimulator.model.Match;
import com.example.footballseasonsimulator.model.Standing;
import com.example.footballseasonsimulator.model.Team;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing league standings.
 */
@Service
public class StandingsService {
    
    // Map: leagueId -> season -> standings
    private final Map<String, Map<Integer, Map<String, Standing>>> allStandings = new ConcurrentHashMap<>();
    
    /**
     * Initialize standings for a new season.
     */
    public void initializeSeason(League league, int season) {
        Map<String, Standing> standings = new ConcurrentHashMap<>();
        for (Team team : league.teams()) {
            standings.put(team.id(), new Standing(team));
        }
        
        allStandings.computeIfAbsent(league.id(), k -> new ConcurrentHashMap<>())
                    .put(season, standings);
    }
    
    /**
     * Update standings after a match.
     */
    public void updateFromMatch(String leagueId, int season, Match match) {
        Map<String, Standing> standings = getStandingsMap(leagueId, season);
        if (standings == null) return;
        
        Standing homeStanding = standings.get(match.getHomeTeam().id());
        Standing awayStanding = standings.get(match.getAwayTeam().id());
        
        if (homeStanding != null) {
            homeStanding.recordResult(match.getHomeScore(), match.getAwayScore());
        }
        if (awayStanding != null) {
            awayStanding.recordResult(match.getAwayScore(), match.getHomeScore());
        }
        
        // Recalculate positions
        recalculatePositions(leagueId, season);
    }
    
    /**
     * Recalculate positions after standings change.
     */
    private void recalculatePositions(String leagueId, int season) {
        Map<String, Standing> standings = getStandingsMap(leagueId, season);
        if (standings == null) return;
        
        List<Standing> sorted = new ArrayList<>(standings.values());
        sorted.sort(null);
        
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setPosition(i + 1);
        }
    }
    
    /**
     * Get sorted standings for a league and season.
     */
    public List<Standing> getStandings(String leagueId, int season) {
        Map<String, Standing> standings = getStandingsMap(leagueId, season);
        if (standings == null) return Collections.emptyList();
        
        List<Standing> sorted = new ArrayList<>(standings.values());
        sorted.sort(null);
        return sorted;
    }
    
    /**
     * Get standing for a specific team.
     */
    public Standing getTeamStanding(String leagueId, int season, String teamId) {
        Map<String, Standing> standings = getStandingsMap(leagueId, season);
        if (standings == null) return null;
        return standings.get(teamId);
    }
    
    private Map<String, Standing> getStandingsMap(String leagueId, int season) {
        Map<Integer, Map<String, Standing>> leagueStandings = allStandings.get(leagueId);
        if (leagueStandings == null) return null;
        return leagueStandings.get(season);
    }
    
    /**
     * Clear all standings (for testing).
     */
    public void clearAll() {
        allStandings.clear();
    }
}

