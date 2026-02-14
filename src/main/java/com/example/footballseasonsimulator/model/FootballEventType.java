package com.example.footballseasonsimulator.model;

/**
 * Enumeration of all possible football match events.
 * Inspired by virtua-football's comprehensive event system.
 */
public enum FootballEventType {
    // Match flow events
    KICK_OFF("Kick Off", false),
    HALF_TIME("Half Time", false),
    SECOND_HALF_KICK_OFF("Second Half Kick Off", false),
    FULL_TIME("Full Time", false),
    
    // Goal events
    GOAL("Goal", true),
    OWN_GOAL("Own Goal", true),
    PENALTY_AWARDED("Penalty Awarded", true),
    PENALTY_SCORED("Penalty Scored", true),
    PENALTY_MISSED("Penalty Missed", true),
    PENALTY_SAVED("Penalty Saved", true),
    
    // Card events
    YELLOW_CARD("Yellow Card", true),
    SECOND_YELLOW("Second Yellow Card", true),
    RED_CARD("Red Card", true),
    
    // Other events
    SUBSTITUTION("Substitution", true),
    INJURY_STOPPAGE("Injury Stoppage", true),
    VAR_CHECK("VAR Check", true),
    VAR_OVERTURNED("VAR Decision Overturned", true),
    CORNER_KICK("Corner Kick", false),
    OFFSIDE("Offside", false),
    SHOT_ON_TARGET("Shot on Target", false),
    SHOT_OFF_TARGET("Shot off Target", false),
    SAVE("Save", false),
    FOUL("Foul", false);

    private final String displayName;
    private final boolean significant;

    FootballEventType(String displayName, boolean significant) {
        this.displayName = displayName;
        this.significant = significant;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this event is significant enough to be shown in match summary.
     */
    public boolean isSignificant() {
        return significant;
    }

    public boolean isGoal() {
        return this == GOAL || this == OWN_GOAL || this == PENALTY_SCORED;
    }

    public boolean isCard() {
        return this == YELLOW_CARD || this == SECOND_YELLOW || this == RED_CARD;
    }
}

