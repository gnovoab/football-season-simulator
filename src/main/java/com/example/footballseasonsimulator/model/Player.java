package com.example.footballseasonsimulator.model;

/**
 * Represents a football player.
 */
public record Player(
    String id,
    String name,
    Position position,
    int shirtNumber,
    int rating  // 1-100 overall rating
) {
    public Player {
        rating = Math.max(1, Math.min(100, rating));
    }
}

