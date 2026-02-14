package com.example.footballseasonsimulator.model;

import java.util.List;

/**
 * Represents a matchweek fixture containing multiple matches.
 */
public record Fixture(
    String leagueId,
    int season,
    int matchweek,
    List<Match> matches
) {
    /**
     * Check if all matches in this fixture are completed.
     */
    public boolean isCompleted() {
        return matches.stream().allMatch(Match::isFinished);
    }

    /**
     * Check if any match is currently live.
     */
    public boolean hasLiveMatches() {
        return matches.stream().anyMatch(Match::isLive);
    }

    /**
     * Get the number of matches in this fixture.
     */
    public int matchCount() {
        return matches.size();
    }
}

