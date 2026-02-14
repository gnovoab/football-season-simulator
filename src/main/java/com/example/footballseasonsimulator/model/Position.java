package com.example.footballseasonsimulator.model;

/**
 * Player positions on the field.
 */
public enum Position {
    GOALKEEPER("GK"),
    DEFENDER("DEF"),
    MIDFIELDER("MID"),
    FORWARD("FWD");

    private final String abbreviation;

    Position(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}

