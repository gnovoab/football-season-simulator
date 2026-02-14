package com.example.footballseasonsimulator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a football match between two teams.
 */
public class Match {
    private final String id;
    private final String leagueId;
    private final int season;
    private final int matchweek;
    private final Team homeTeam;
    private final Team awayTeam;
    
    private int homeScore;
    private int awayScore;
    private MatchPhase phase;
    private int matchMinute;
    private int additionalMinutes;
    private final List<MatchEvent> events;

    public Match(String leagueId, int season, int matchweek, Team homeTeam, Team awayTeam) {
        this.id = UUID.randomUUID().toString();
        this.leagueId = leagueId;
        this.season = season;
        this.matchweek = matchweek;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = 0;
        this.awayScore = 0;
        this.phase = MatchPhase.NOT_STARTED;
        this.matchMinute = 0;
        this.additionalMinutes = 0;
        this.events = new ArrayList<>();
    }

    public void addEvent(MatchEvent event) {
        events.add(event);
        
        // Update score if it's a goal
        if (event.type().isGoal()) {
            if (event.teamId() != null) {
                if (event.teamId().equals(homeTeam.id())) {
                    homeScore++;
                } else if (event.teamId().equals(awayTeam.id())) {
                    awayScore++;
                }
            }
        }
    }

    public void setPhase(MatchPhase phase) {
        this.phase = phase;
    }

    public void setMatchMinute(int minute) {
        this.matchMinute = minute;
    }

    public void setAdditionalMinutes(int additionalMinutes) {
        this.additionalMinutes = additionalMinutes;
    }

    public String getScoreDisplay() {
        return homeScore + " - " + awayScore;
    }

    public String getTimeDisplay() {
        if (phase == MatchPhase.NOT_STARTED) return "Not Started";
        if (phase == MatchPhase.HALF_TIME) return "HT";
        if (phase == MatchPhase.FULL_TIME) return "FT";
        if (additionalMinutes > 0) {
            return matchMinute + "+" + additionalMinutes + "'";
        }
        return matchMinute + "'";
    }

    public List<MatchEvent> getSignificantEvents() {
        return events.stream()
            .filter(e -> e.type().isSignificant())
            .toList();
    }

    // Getters
    public String getId() { return id; }
    public String getLeagueId() { return leagueId; }
    public int getSeason() { return season; }
    public int getMatchweek() { return matchweek; }
    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public MatchPhase getPhase() { return phase; }
    public int getMatchMinute() { return matchMinute; }
    public int getAdditionalMinutes() { return additionalMinutes; }
    public List<MatchEvent> getEvents() { return new ArrayList<>(events); }
    
    public boolean isFinished() {
        return phase == MatchPhase.FULL_TIME;
    }

    public boolean isLive() {
        return phase.isPlaying();
    }
}

