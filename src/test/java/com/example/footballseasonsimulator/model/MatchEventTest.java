package com.example.footballseasonsimulator.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MatchEvent record.
 */
class MatchEventTest {

    @Test
    @DisplayName("simple should create event without player")
    void simpleShouldCreateEventWithoutPlayer() {
        MatchEvent event = MatchEvent.simple(45, FootballEventType.HALF_TIME, "Half Time");

        assertThat(event.minute()).isEqualTo(45);
        assertThat(event.additionalMinutes()).isZero();
        assertThat(event.type()).isEqualTo(FootballEventType.HALF_TIME);
        assertThat(event.teamId()).isNull();
        assertThat(event.playerId()).isNull();
        assertThat(event.playerName()).isNull();
        assertThat(event.description()).isEqualTo("Half Time");
        assertThat(event.id()).isNotBlank();
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("withPlayer should create event with team and player")
    void withPlayerShouldCreateEventWithTeamAndPlayer() {
        Team team = new Team("arsenal", "Arsenal", "ARS", "/badge.png",
                new TeamStrength(85, 85, 85, 85), List.of());
        Player player = new Player("saka", "Bukayo Saka", Position.FORWARD, 7, 88);

        MatchEvent event = MatchEvent.withPlayer(67, 0, FootballEventType.GOAL, team, player, "Goal!");

        assertThat(event.minute()).isEqualTo(67);
        assertThat(event.type()).isEqualTo(FootballEventType.GOAL);
        assertThat(event.teamId()).isEqualTo("arsenal");
        assertThat(event.playerId()).isEqualTo("saka");
        assertThat(event.playerName()).isEqualTo("Bukayo Saka");
    }

    @Test
    @DisplayName("withPlayer should handle null team and player")
    void withPlayerShouldHandleNullTeamAndPlayer() {
        MatchEvent event = MatchEvent.withPlayer(30, 0, FootballEventType.FOUL, null, null, "Foul");

        assertThat(event.teamId()).isNull();
        assertThat(event.playerId()).isNull();
        assertThat(event.playerName()).isNull();
    }

    @Test
    @DisplayName("getDisplayTime should return minute with apostrophe")
    void getDisplayTimeShouldReturnMinuteWithApostrophe() {
        MatchEvent event = MatchEvent.simple(30, FootballEventType.SHOT_ON_TARGET, "Shot");

        assertThat(event.getDisplayTime()).isEqualTo("30'");
    }

    @Test
    @DisplayName("getDisplayTime should include additional minutes")
    void getDisplayTimeShouldIncludeAdditionalMinutes() {
        Team team = new Team("team", "Team", "TM", "/badge.png",
                new TeamStrength(80, 80, 80, 80), List.of());
        MatchEvent event = MatchEvent.withPlayer(45, 3, FootballEventType.GOAL, team, null, "Goal!");

        assertThat(event.getDisplayTime()).isEqualTo("45+3'");
    }

    @Test
    @DisplayName("Each event should have unique ID")
    void eachEventShouldHaveUniqueId() {
        MatchEvent event1 = MatchEvent.simple(30, FootballEventType.FOUL, "Foul");
        MatchEvent event2 = MatchEvent.simple(30, FootballEventType.FOUL, "Foul");

        assertThat(event1.id()).isNotEqualTo(event2.id());
    }
}

