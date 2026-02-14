package com.example.footballseasonsimulator.model;

/**
 * Represents the current state of a season simulation.
 */
public enum SeasonState {
    IDLE("Idle - Waiting to start"),
    COUNTDOWN("Countdown to kick-off"),
    RUNNING_FIXTURE("Running fixture matches"),
    WAITING_NEXT_FIXTURE("Waiting for next fixture"),
    SEASON_COMPLETE("Season completed"),
    WAITING_NEXT_SEASON("Waiting for next season to start");

    private final String description;

    SeasonState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

