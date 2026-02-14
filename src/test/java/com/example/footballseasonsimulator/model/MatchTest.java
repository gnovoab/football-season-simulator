package com.example.footballseasonsimulator.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Match model.
 */
class MatchTest {

    private Match match;
    private Team homeTeam;
    private Team awayTeam;

    @BeforeEach
    void setUp() {
        homeTeam = new Team("home", "Home Team", "HOM", "/home.png",
                new TeamStrength(80, 80, 80, 80), List.of());
        awayTeam = new Team("away", "Away Team", "AWY", "/away.png",
                new TeamStrength(75, 75, 75, 75), List.of());
        match = new Match("premier-league", 2025, 1, homeTeam, awayTeam);
    }

    @Test
    @DisplayName("Match should have unique ID")
    void matchShouldHaveUniqueId() {
        Match match2 = new Match("premier-league", 2025, 1, homeTeam, awayTeam);
        assertThat(match.getId()).isNotEqualTo(match2.getId());
    }

    @Test
    @DisplayName("Match should start with NOT_STARTED phase")
    void matchShouldStartWithNotStartedPhase() {
        assertThat(match.getPhase()).isEqualTo(MatchPhase.NOT_STARTED);
    }

    @Test
    @DisplayName("Match should start with 0-0 score")
    void matchShouldStartWithZeroScore() {
        assertThat(match.getHomeScore()).isZero();
        assertThat(match.getAwayScore()).isZero();
    }

    @Test
    @DisplayName("addEvent should increment home score for home goal")
    void addEventShouldIncrementHomeScore() {
        MatchEvent goal = MatchEvent.withPlayer(30, 0, FootballEventType.GOAL, homeTeam, null, "Goal!");
        match.addEvent(goal);

        assertThat(match.getHomeScore()).isEqualTo(1);
        assertThat(match.getAwayScore()).isZero();
    }

    @Test
    @DisplayName("addEvent should increment away score for away goal")
    void addEventShouldIncrementAwayScore() {
        MatchEvent goal = MatchEvent.withPlayer(30, 0, FootballEventType.GOAL, awayTeam, null, "Goal!");
        match.addEvent(goal);

        assertThat(match.getHomeScore()).isZero();
        assertThat(match.getAwayScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("addEvent should increment score for penalty scored")
    void addEventShouldIncrementScoreForPenalty() {
        MatchEvent penalty = MatchEvent.withPlayer(30, 0, FootballEventType.PENALTY_SCORED, homeTeam, null, "Penalty!");
        match.addEvent(penalty);

        assertThat(match.getHomeScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("getScoreDisplay should return formatted score")
    void getScoreDisplayShouldReturnFormattedScore() {
        assertThat(match.getScoreDisplay()).isEqualTo("0 - 0");

        match.addEvent(MatchEvent.withPlayer(30, 0, FootballEventType.GOAL, homeTeam, null, "Goal!"));
        match.addEvent(MatchEvent.withPlayer(45, 0, FootballEventType.GOAL, homeTeam, null, "Goal!"));
        match.addEvent(MatchEvent.withPlayer(60, 0, FootballEventType.GOAL, awayTeam, null, "Goal!"));

        assertThat(match.getScoreDisplay()).isEqualTo("2 - 1");
    }

    @Test
    @DisplayName("getTimeDisplay should return 'Not Started' before match")
    void getTimeDisplayShouldReturnNotStarted() {
        assertThat(match.getTimeDisplay()).isEqualTo("Not Started");
    }

    @Test
    @DisplayName("getTimeDisplay should return 'HT' at half time")
    void getTimeDisplayShouldReturnHT() {
        match.setPhase(MatchPhase.HALF_TIME);
        assertThat(match.getTimeDisplay()).isEqualTo("HT");
    }

    @Test
    @DisplayName("getTimeDisplay should return 'FT' at full time")
    void getTimeDisplayShouldReturnFT() {
        match.setPhase(MatchPhase.FULL_TIME);
        assertThat(match.getTimeDisplay()).isEqualTo("FT");
    }

    @Test
    @DisplayName("getTimeDisplay should return minute during play")
    void getTimeDisplayShouldReturnMinute() {
        match.setPhase(MatchPhase.FIRST_HALF);
        match.setMatchMinute(30);
        assertThat(match.getTimeDisplay()).isEqualTo("30'");
    }

    @Test
    @DisplayName("getTimeDisplay should include additional time")
    void getTimeDisplayShouldIncludeAdditionalTime() {
        match.setPhase(MatchPhase.FIRST_HALF);
        match.setMatchMinute(45);
        match.setAdditionalMinutes(2);
        assertThat(match.getTimeDisplay()).isEqualTo("45+2'");
    }

    @Test
    @DisplayName("isFinished should return true only at FULL_TIME")
    void isFinishedShouldReturnTrueOnlyAtFullTime() {
        assertThat(match.isFinished()).isFalse();
        match.setPhase(MatchPhase.FIRST_HALF);
        assertThat(match.isFinished()).isFalse();
        match.setPhase(MatchPhase.FULL_TIME);
        assertThat(match.isFinished()).isTrue();
    }

    @Test
    @DisplayName("isLive should return true during play")
    void isLiveShouldReturnTrueDuringPlay() {
        assertThat(match.isLive()).isFalse();
        match.setPhase(MatchPhase.FIRST_HALF);
        assertThat(match.isLive()).isTrue();
        match.setPhase(MatchPhase.SECOND_HALF);
        assertThat(match.isLive()).isTrue();
        match.setPhase(MatchPhase.HALF_TIME);
        assertThat(match.isLive()).isFalse();
    }

    @Test
    @DisplayName("getSignificantEvents should filter events")
    void getSignificantEventsShouldFilterEvents() {
        match.addEvent(MatchEvent.simple(1, FootballEventType.KICK_OFF, "Kick off")); // not significant
        match.addEvent(MatchEvent.simple(30, FootballEventType.SHOT_ON_TARGET, "Shot")); // not significant
        match.addEvent(MatchEvent.withPlayer(45, 0, FootballEventType.GOAL, homeTeam, null, "Goal!")); // significant
        match.addEvent(MatchEvent.withPlayer(60, 0, FootballEventType.YELLOW_CARD, awayTeam, null, "Yellow!")); // significant

        List<MatchEvent> significant = match.getSignificantEvents();
        assertThat(significant).hasSize(2); // GOAL and YELLOW_CARD are significant
    }
}

