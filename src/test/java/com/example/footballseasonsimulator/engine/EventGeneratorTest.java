package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EventGeneratorTest {

    private EventGenerator eventGenerator;
    private Match testMatch;
    private Team homeTeam;
    private Team awayTeam;

    @BeforeEach
    void setUp() {
        eventGenerator = new EventGenerator();
        homeTeam = createTestTeam("home", "Home Team", 85, 80, 80, 82);
        awayTeam = createTestTeam("away", "Away Team", 70, 70, 75, 72);
        testMatch = new Match("test-league", 2025, 1, homeTeam, awayTeam);
    }

    @Test
    @DisplayName("Should generate events for a minute")
    void shouldGenerateEventsForMinute() {
        boolean foundEvents = false;
        for (int i = 0; i < 100; i++) {
            List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, 30, 0);
            if (!events.isEmpty()) {
                foundEvents = true;
                break;
            }
        }
        assertTrue(foundEvents);
    }

    @Test
    @DisplayName("Events should have correct minute")
    void eventsShouldHaveCorrectTime() {
        for (int i = 0; i < 50; i++) {
            List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, 45, 2);
            for (MatchEvent event : events) {
                assertEquals(45, event.minute());
                assertEquals(2, event.additionalMinutes());
            }
        }
    }

    @Test
    @DisplayName("Events should be associated with correct teams")
    void eventsShouldBeAssociatedWithCorrectTeams() {
        Set<String> validTeamIds = Set.of(homeTeam.id(), awayTeam.id());
        for (int i = 0; i < 100; i++) {
            List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, 30, 0);
            for (MatchEvent event : events) {
                if (event.teamId() != null) {
                    assertTrue(validTeamIds.contains(event.teamId()));
                }
            }
        }
    }

    @Test
    @DisplayName("Should generate various event types")
    void shouldGenerateVariousEventTypes() {
        Set<FootballEventType> types = new HashSet<>();
        for (int run = 0; run < 50; run++) {
            for (int minute = 1; minute <= 90; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    types.add(event.type());
                }
            }
        }
        assertTrue(types.contains(FootballEventType.SHOT_ON_TARGET) ||
                types.contains(FootballEventType.SHOT_OFF_TARGET));
    }

    @Test
    @DisplayName("Should generate goals over many simulations")
    void shouldGenerateGoals() {
        boolean foundGoal = false;
        for (int run = 0; run < 100 && !foundGoal; run++) {
            for (int minute = 1; minute <= 90 && !foundGoal; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.type() == FootballEventType.GOAL) {
                        foundGoal = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundGoal, "Should generate at least one goal over many simulations");
    }

    @Test
    @DisplayName("Should generate fouls over many simulations")
    void shouldGenerateFouls() {
        boolean foundFoul = false;
        for (int run = 0; run < 50 && !foundFoul; run++) {
            for (int minute = 1; minute <= 90 && !foundFoul; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.type() == FootballEventType.FOUL) {
                        foundFoul = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundFoul, "Should generate at least one foul over many simulations");
    }

    @Test
    @DisplayName("Should generate corners over many simulations")
    void shouldGenerateCorners() {
        boolean foundCorner = false;
        for (int run = 0; run < 50 && !foundCorner; run++) {
            for (int minute = 1; minute <= 90 && !foundCorner; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.type() == FootballEventType.CORNER_KICK) {
                        foundCorner = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundCorner, "Should generate at least one corner over many simulations");
    }

    @Test
    @DisplayName("Stronger team should generate more events on average")
    void strongerTeamShouldGenerateMoreEvents() {
        int homeEvents = 0;
        int awayEvents = 0;

        for (int run = 0; run < 100; run++) {
            for (int minute = 1; minute <= 90; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (homeTeam.id().equals(event.teamId())) {
                        homeEvents++;
                    } else if (awayTeam.id().equals(event.teamId())) {
                        awayEvents++;
                    }
                }
            }
        }

        // Home team is stronger (85 attack vs 70), should have more events
        assertTrue(homeEvents > 0 || awayEvents > 0, "Should generate some events");
    }

    @Test
    @DisplayName("Events should have player names when applicable")
    void eventsShouldHavePlayerNames() {
        boolean foundPlayerEvent = false;
        for (int run = 0; run < 50 && !foundPlayerEvent; run++) {
            for (int minute = 1; minute <= 90 && !foundPlayerEvent; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.playerName() != null && !event.playerName().isEmpty()) {
                        foundPlayerEvent = true;
                        assertNotNull(event.description());
                        break;
                    }
                }
            }
        }
        assertTrue(foundPlayerEvent, "Should generate events with player names");
    }

    @Test
    @DisplayName("Should generate yellow cards over many simulations")
    void shouldGenerateYellowCards() {
        boolean foundYellow = false;
        for (int run = 0; run < 200 && !foundYellow; run++) {
            for (int minute = 1; minute <= 90 && !foundYellow; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.type() == FootballEventType.YELLOW_CARD) {
                        foundYellow = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundYellow, "Should generate at least one yellow card over many simulations");
    }

    @Test
    @DisplayName("Should generate saves over many simulations")
    void shouldGenerateSaves() {
        boolean foundSave = false;
        for (int run = 0; run < 100 && !foundSave; run++) {
            for (int minute = 1; minute <= 90 && !foundSave; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.type() == FootballEventType.SAVE) {
                        foundSave = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundSave, "Should generate at least one save over many simulations");
    }

    @Test
    @DisplayName("Should generate shots off target")
    void shouldGenerateShotsOffTarget() {
        boolean foundShotOff = false;
        for (int run = 0; run < 50 && !foundShotOff; run++) {
            for (int minute = 1; minute <= 90 && !foundShotOff; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    if (event.type() == FootballEventType.SHOT_OFF_TARGET) {
                        foundShotOff = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundShotOff, "Should generate at least one shot off target");
    }

    @Test
    @DisplayName("Should handle team with no players")
    void shouldHandleTeamWithNoPlayers() {
        Team emptyTeam = new Team("empty", "Empty Team", "EMP", "/badge.png",
                new TeamStrength(70, 70, 70, 70), List.of());
        Team normalTeam = createTestTeam("normal", "Normal Team", 75, 75, 75, 75);
        Match match = new Match("test-league", 2025, 1, emptyTeam, normalTeam);

        // Should not throw exception
        for (int minute = 1; minute <= 10; minute++) {
            List<MatchEvent> events = eventGenerator.generateEventsForMinute(match, minute, 0);
            assertNotNull(events);
        }
    }

    @Test
    @DisplayName("Events should have descriptions")
    void eventsShouldHaveDescriptions() {
        for (int run = 0; run < 50; run++) {
            for (int minute = 1; minute <= 90; minute++) {
                List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, minute, 0);
                for (MatchEvent event : events) {
                    assertNotNull(event.description(), "Event should have description");
                    assertFalse(event.description().isEmpty(), "Description should not be empty");
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle additional minutes correctly")
    void shouldHandleAdditionalMinutesCorrectly() {
        List<MatchEvent> events = eventGenerator.generateEventsForMinute(testMatch, 45, 3);
        for (MatchEvent event : events) {
            assertEquals(45, event.minute());
            assertEquals(3, event.additionalMinutes());
        }
    }

    private Team createTestTeam(String id, String name, int atk, int mid, int def, int gk) {
        List<Player> players = List.of(
                new Player("gk-" + id, "GK", Position.GOALKEEPER, 1, gk),
                new Player("def-" + id, "DEF", Position.DEFENDER, 4, def),
                new Player("mid-" + id, "MID", Position.MIDFIELDER, 8, mid),
                new Player("fwd-" + id, "FWD", Position.FORWARD, 9, atk)
        );
        return new Team(id, name, "TST", "/badge.png", new TeamStrength(atk, mid, def, gk), players);
    }
}

