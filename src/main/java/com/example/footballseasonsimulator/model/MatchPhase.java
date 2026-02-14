package com.example.footballseasonsimulator.model;

/**
 * Represents the current phase of a football match.
 */
public enum MatchPhase {
    NOT_STARTED("Not Started"),
    FIRST_HALF("First Half"),
    HALF_TIME("Half Time"),
    SECOND_HALF("Second Half"),
    FULL_TIME("Full Time");

    private final String displayName;

    MatchPhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPlaying() {
        return this == FIRST_HALF || this == SECOND_HALF;
    }

    public boolean isFinished() {
        return this == FULL_TIME;
    }
}

