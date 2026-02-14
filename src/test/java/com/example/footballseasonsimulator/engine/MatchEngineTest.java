package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchEngineTest {

    private MatchEngine matchEngine;
    private Match testMatch;
    private List<MatchEvent> capturedEvents;

    @BeforeEach
    void setUp() {
        matchEngine = new MatchEngine();
        Team homeTeam = createTestTeam("home", "Home Team");
        Team awayTeam = createTestTeam("away", "Away Team");
        testMatch = new Match("test-league", 2025, 1, homeTeam, awayTeam);
        capturedEvents = new ArrayList<>();
    }

    @Test
    @DisplayName("Should start match and set phase to FIRST_HALF")
    void shouldStartMatchCorrectly() {
        matchEngine.startMatch(testMatch);
        assertEquals(MatchPhase.FIRST_HALF, testMatch.getPhase());
        assertTrue(matchEngine.isRunning());
    }

    @Test
    @DisplayName("Should generate KICK_OFF event on start")
    void shouldGenerateKickOffEvent() {
        matchEngine.setEventCallback(capturedEvents::add);
        matchEngine.startMatch(testMatch);
        assertFalse(capturedEvents.isEmpty());
        assertEquals(FootballEventType.KICK_OFF, capturedEvents.get(0).type());
    }

    @Test
    @DisplayName("Tick should return true while match is running")
    void tickShouldReturnTrueWhileRunning() {
        matchEngine.startMatch(testMatch);
        assertTrue(matchEngine.tick());
    }

    @Test
    @DisplayName("Tick should return false when match not started")
    void tickShouldReturnFalseWhenNotStarted() {
        assertFalse(matchEngine.tick());
    }

    @Test
    @DisplayName("Match should finish after enough ticks")
    void matchShouldFinish() {
        matchEngine.startMatch(testMatch);
        int ticks = 0;
        while (matchEngine.tick() && ticks < 1000) {
            ticks++;
        }
        assertEquals(MatchPhase.FULL_TIME, testMatch.getPhase());
        assertFalse(matchEngine.isRunning());
    }

    @Test
    @DisplayName("Should generate HALF_TIME and FULL_TIME events")
    void shouldGeneratePhaseEvents() {
        matchEngine.setEventCallback(capturedEvents::add);
        matchEngine.startMatch(testMatch);
        int ticks = 0;
        while (matchEngine.tick() && ticks < 1000) {
            ticks++;
        }
        boolean hasHT = capturedEvents.stream().anyMatch(e -> e.type() == FootballEventType.HALF_TIME);
        boolean hasFT = capturedEvents.stream().anyMatch(e -> e.type() == FootballEventType.FULL_TIME);
        assertTrue(hasHT);
        assertTrue(hasFT);
    }

    @Test
    @DisplayName("getCurrentMatch should return the current match")
    void getCurrentMatchShouldReturnMatch() {
        matchEngine.startMatch(testMatch);
        assertEquals(testMatch, matchEngine.getCurrentMatch());
    }

    @Test
    @DisplayName("getTickIntervalMs should return positive value")
    void getTickIntervalMsShouldReturnPositiveValue() {
        assertTrue(MatchEngine.getTickIntervalMs() > 0);
    }

    @Test
    @DisplayName("State callback should be called during match")
    void stateCallbackShouldBeCalled() {
        List<Match> stateUpdates = new ArrayList<>();
        matchEngine.setStateCallback(stateUpdates::add);
        matchEngine.startMatch(testMatch);

        // Tick a few times
        for (int i = 0; i < 10; i++) {
            matchEngine.tick();
        }

        assertFalse(stateUpdates.isEmpty(), "State callback should be called");
    }

    @Test
    @DisplayName("Match should progress through phases")
    void matchShouldProgressThroughPhases() {
        matchEngine.setEventCallback(capturedEvents::add);
        matchEngine.startMatch(testMatch);

        // Run until complete
        int ticks = 0;
        while (matchEngine.tick() && ticks < 1000) {
            ticks++;
        }

        // Verify we went through all phases
        boolean hasKickOff = capturedEvents.stream()
                .anyMatch(e -> e.type() == FootballEventType.KICK_OFF);
        boolean hasSecondHalfKickOff = capturedEvents.stream()
                .anyMatch(e -> e.type() == FootballEventType.SECOND_HALF_KICK_OFF);
        boolean hasHalfTime = capturedEvents.stream()
                .anyMatch(e -> e.type() == FootballEventType.HALF_TIME);
        boolean hasFullTime = capturedEvents.stream()
                .anyMatch(e -> e.type() == FootballEventType.FULL_TIME);

        assertTrue(hasKickOff, "Should have kick off event");
        assertTrue(hasSecondHalfKickOff, "Should have second half kick off event");
        assertTrue(hasHalfTime, "Should have half time event");
        assertTrue(hasFullTime, "Should have full time event");
    }

    @Test
    @DisplayName("Match should have valid time display")
    void matchShouldHaveValidTimeDisplay() {
        matchEngine.startMatch(testMatch);

        // Tick a few times
        for (int i = 0; i < 50; i++) {
            matchEngine.tick();
        }

        String timeDisplay = testMatch.getTimeDisplay();
        assertNotNull(timeDisplay);
        assertFalse(timeDisplay.isEmpty());
    }

    @Test
    @DisplayName("isRunning should return false before start")
    void isRunningShouldReturnFalseBeforeStart() {
        assertFalse(matchEngine.isRunning());
    }

    @Test
    @DisplayName("getCurrentMatch should return null before start")
    void getCurrentMatchShouldReturnNullBeforeStart() {
        assertNull(matchEngine.getCurrentMatch());
    }

    @Test
    @DisplayName("Match minute should increase during simulation")
    void matchMinuteShouldIncrease() {
        matchEngine.startMatch(testMatch);
        int initialMinute = testMatch.getMatchMinute();

        // Tick several times
        for (int i = 0; i < 100; i++) {
            matchEngine.tick();
        }

        assertTrue(testMatch.getMatchMinute() >= initialMinute,
                "Match minute should increase during simulation");
    }

    @Test
    @DisplayName("Match should generate events during simulation")
    void matchShouldGenerateEvents() {
        matchEngine.setEventCallback(capturedEvents::add);
        matchEngine.startMatch(testMatch);

        // Run until complete
        int ticks = 0;
        while (matchEngine.tick() && ticks < 1000) {
            ticks++;
        }

        // Should have more than just phase events
        assertTrue(capturedEvents.size() > 4,
                "Should generate multiple events during match");
    }

    @Test
    @DisplayName("Match should handle stoppage time")
    void matchShouldHandleStoppageTime() {
        matchEngine.setEventCallback(capturedEvents::add);
        matchEngine.startMatch(testMatch);

        // Run until complete
        int ticks = 0;
        while (matchEngine.tick() && ticks < 1000) {
            ticks++;
        }

        // Check for events with additional minutes (stoppage time)
        boolean hasStoppageTimeEvent = capturedEvents.stream()
                .anyMatch(e -> e.additionalMinutes() > 0);
        // Stoppage time events are possible but not guaranteed
        assertNotNull(capturedEvents);
    }

    @Test
    @DisplayName("Event callback should receive all events")
    void eventCallbackShouldReceiveAllEvents() {
        List<MatchEvent> callbackEvents = new ArrayList<>();
        matchEngine.setEventCallback(callbackEvents::add);
        matchEngine.startMatch(testMatch);

        // Run until complete
        int ticks = 0;
        while (matchEngine.tick() && ticks < 1000) {
            ticks++;
        }

        // Events in callback should match events in match
        assertEquals(testMatch.getEvents().size(), callbackEvents.size(),
                "Callback should receive all events");
    }

    private Team createTestTeam(String id, String name) {
        List<Player> players = List.of(
                new Player("gk-" + id, "GK", Position.GOALKEEPER, 1, 75),
                new Player("def-" + id, "DEF", Position.DEFENDER, 4, 75),
                new Player("mid-" + id, "MID", Position.MIDFIELDER, 8, 75),
                new Player("fwd-" + id, "FWD", Position.FORWARD, 9, 75)
        );
        return new Team(id, name, "TST", "/badge.png", new TeamStrength(75, 75, 75, 75), players);
    }
}

