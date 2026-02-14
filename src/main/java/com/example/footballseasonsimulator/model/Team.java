package com.example.footballseasonsimulator.model;

import java.util.List;

/**
 * Represents a football team with its players and strength ratings.
 */
public record Team(
    String id,
    String name,
    String shortName,
    String badgeUrl,
    TeamStrength strength,
    List<Player> players
) {
    /**
     * Get players by position.
     */
    public List<Player> getPlayersByPosition(Position position) {
        return players.stream()
            .filter(p -> p.position() == position)
            .toList();
    }

    /**
     * Get the starting goalkeeper.
     */
    public Player getGoalkeeper() {
        return players.stream()
            .filter(p -> p.position() == Position.GOALKEEPER)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get forwards for goal scoring events.
     */
    public List<Player> getForwards() {
        return getPlayersByPosition(Position.FORWARD);
    }

    /**
     * Get midfielders.
     */
    public List<Player> getMidfielders() {
        return getPlayersByPosition(Position.MIDFIELDER);
    }

    /**
     * Get defenders.
     */
    public List<Player> getDefenders() {
        return getPlayersByPosition(Position.DEFENDER);
    }
}

