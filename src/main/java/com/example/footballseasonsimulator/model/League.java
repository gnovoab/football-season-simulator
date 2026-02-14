package com.example.footballseasonsimulator.model;

import java.util.List;

/**
 * Represents a football league with its teams.
 */
public record League(
    String id,
    String name,
    String country,
    String logoUrl,
    List<Team> teams
) {
    /**
     * Get the number of teams in the league.
     */
    public int teamCount() {
        return teams != null ? teams.size() : 0;
    }

    /**
     * Calculate total matchweeks for a double round-robin.
     * Each team plays every other team twice (home and away).
     */
    public int totalMatchweeks() {
        int n = teamCount();
        return (n - 1) * 2; // 38 matchweeks for 20 teams
    }

    /**
     * Calculate matches per matchweek.
     */
    public int matchesPerMatchweek() {
        return teamCount() / 2; // 10 matches for 20 teams
    }
}

