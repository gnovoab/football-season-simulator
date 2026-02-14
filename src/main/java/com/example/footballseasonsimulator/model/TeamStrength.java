package com.example.footballseasonsimulator.model;

/**
 * Represents a team's strength ratings across different aspects of play.
 * Each rating is on a scale of 1-100.
 */
public record TeamStrength(
    int attack,
    int midfield,
    int defense,
    int goalkeeper
) {
    public TeamStrength {
        // Validate ratings are within bounds
        attack = Math.max(1, Math.min(100, attack));
        midfield = Math.max(1, Math.min(100, midfield));
        defense = Math.max(1, Math.min(100, defense));
        goalkeeper = Math.max(1, Math.min(100, goalkeeper));
    }

    /**
     * Calculate overall team strength as weighted average.
     */
    public int overall() {
        return (attack * 30 + midfield * 25 + defense * 30 + goalkeeper * 15) / 100;
    }

    /**
     * Create a default mid-tier team strength.
     */
    public static TeamStrength defaultStrength() {
        return new TeamStrength(70, 70, 70, 70);
    }
}

