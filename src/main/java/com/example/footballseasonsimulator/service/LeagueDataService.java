package com.example.footballseasonsimulator.service;

import com.example.footballseasonsimulator.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Service for loading and managing league and team data.
 */
@Service
public class LeagueDataService {
    
    private static final Logger log = LoggerFactory.getLogger(LeagueDataService.class);
    
    private final ObjectMapper objectMapper;
    private final Map<String, League> leagues = new LinkedHashMap<>();
    
    public LeagueDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() {
        loadLeagues();
    }
    
    private void loadLeagues() {
        String[] leagueFiles = {
            "premier-league",
            "la-liga",
            "serie-a",
            "bundesliga",
            "ligue-1"
        };
        
        for (String leagueFile : leagueFiles) {
            try {
                League league = loadLeagueFromFile(leagueFile);
                leagues.put(league.id(), league);
                log.info("Loaded league: {} with {} teams", league.name(), league.teamCount());
            } catch (IOException e) {
                log.error("Failed to load league: {}", leagueFile, e);
            }
        }
    }
    
    private League loadLeagueFromFile(String filename) throws IOException {
        String path = "data/" + filename + ".json";
        ClassPathResource resource = new ClassPathResource(path);
        
        try (InputStream is = resource.getInputStream()) {
            LeagueData data = objectMapper.readValue(is, LeagueData.class);
            
            List<Team> teams = new ArrayList<>();
            for (TeamData teamData : data.teams) {
                List<Player> players = new ArrayList<>();
                for (PlayerData pd : teamData.players) {
                    players.add(new Player(
                        pd.id,
                        pd.name,
                        Position.valueOf(pd.position.toUpperCase()),
                        pd.shirtNumber,
                        pd.rating
                    ));
                }
                
                TeamStrength strength = new TeamStrength(
                    teamData.strength.attack,
                    teamData.strength.midfield,
                    teamData.strength.defense,
                    teamData.strength.goalkeeper
                );
                
                teams.add(new Team(
                    teamData.id,
                    teamData.name,
                    teamData.shortName,
                    teamData.badgeUrl,
                    strength,
                    players
                ));
            }
            
            return new League(data.id, data.name, data.country, data.logoUrl, teams);
        }
    }
    
    public List<League> getAllLeagues() {
        return new ArrayList<>(leagues.values());
    }
    
    public League getLeague(String leagueId) {
        return leagues.get(leagueId);
    }
    
    public Team getTeam(String leagueId, String teamId) {
        League league = leagues.get(leagueId);
        if (league == null) return null;
        return league.teams().stream()
            .filter(t -> t.id().equals(teamId))
            .findFirst()
            .orElse(null);
    }
    
    // Data classes for JSON parsing
    private static class LeagueData {
        public String id;
        public String name;
        public String country;
        public String logoUrl;
        public List<TeamData> teams;
    }
    
    private static class TeamData {
        public String id;
        public String name;
        public String shortName;
        public String badgeUrl;
        public StrengthData strength;
        public List<PlayerData> players;
    }
    
    private static class StrengthData {
        public int attack;
        public int midfield;
        public int defense;
        public int goalkeeper;
    }
    
    private static class PlayerData {
        public String id;
        public String name;
        public String position;
        public int shirtNumber;
        public int rating;
    }
}

