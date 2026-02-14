package com.example.footballseasonsimulator.model;

import java.time.Instant;

/**
 * Represents an event that occurred during a match.
 */
public record MatchEvent(
    String id,
    int minute,
    int additionalMinutes,  // For stoppage time (e.g., 45+2)
    FootballEventType type,
    String teamId,
    String playerId,
    String playerName,
    String description,
    Instant timestamp
) {
    /**
     * Get display time (e.g., "45+2'" or "67'")
     */
    public String getDisplayTime() {
        if (additionalMinutes > 0) {
            return minute + "+" + additionalMinutes + "'";
        }
        return minute + "'";
    }

    /**
     * Create a simple event without player involvement.
     */
    public static MatchEvent simple(int minute, FootballEventType type, String description) {
        return new MatchEvent(
            java.util.UUID.randomUUID().toString(),
            minute,
            0,
            type,
            null,
            null,
            null,
            description,
            Instant.now()
        );
    }

    /**
     * Create an event with team and player.
     */
    public static MatchEvent withPlayer(int minute, int additionalMinutes, FootballEventType type,
                                         Team team, Player player, String description) {
        return new MatchEvent(
            java.util.UUID.randomUUID().toString(),
            minute,
            additionalMinutes,
            type,
            team != null ? team.id() : null,
            player != null ? player.id() : null,
            player != null ? player.name() : null,
            description,
            Instant.now()
        );
    }
}

