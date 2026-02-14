package com.example.footballseasonsimulator.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Standing model.
 */
class StandingTest {

    private Standing standing;
    private Team team;

    @BeforeEach
    void setUp() {
        team = new Team("team1", "Team One", "T1", "/badge.png",
                new TeamStrength(80, 80, 80, 80), List.of());
        standing = new Standing(team);
    }

    @Test
    @DisplayName("Standing should initialize with zero values")
    void standingShouldInitializeWithZeroValues() {
        assertThat(standing.getPlayed()).isZero();
        assertThat(standing.getWon()).isZero();
        assertThat(standing.getDrawn()).isZero();
        assertThat(standing.getLost()).isZero();
        assertThat(standing.getGoalsFor()).isZero();
        assertThat(standing.getGoalsAgainst()).isZero();
        assertThat(standing.getPoints()).isZero();
    }

    @Test
    @DisplayName("recordResult should update stats for win")
    void recordResultShouldUpdateStatsForWin() {
        standing.recordResult(3, 1);

        assertThat(standing.getPlayed()).isEqualTo(1);
        assertThat(standing.getWon()).isEqualTo(1);
        assertThat(standing.getDrawn()).isZero();
        assertThat(standing.getLost()).isZero();
        assertThat(standing.getGoalsFor()).isEqualTo(3);
        assertThat(standing.getGoalsAgainst()).isEqualTo(1);
        assertThat(standing.getPoints()).isEqualTo(3);
    }

    @Test
    @DisplayName("recordResult should update stats for draw")
    void recordResultShouldUpdateStatsForDraw() {
        standing.recordResult(2, 2);

        assertThat(standing.getPlayed()).isEqualTo(1);
        assertThat(standing.getWon()).isZero();
        assertThat(standing.getDrawn()).isEqualTo(1);
        assertThat(standing.getLost()).isZero();
        assertThat(standing.getPoints()).isEqualTo(1);
    }

    @Test
    @DisplayName("recordResult should update stats for loss")
    void recordResultShouldUpdateStatsForLoss() {
        standing.recordResult(0, 2);

        assertThat(standing.getPlayed()).isEqualTo(1);
        assertThat(standing.getWon()).isZero();
        assertThat(standing.getDrawn()).isZero();
        assertThat(standing.getLost()).isEqualTo(1);
        assertThat(standing.getPoints()).isZero();
    }

    @Test
    @DisplayName("getGoalDifference should calculate correctly")
    void getGoalDifferenceShouldCalculateCorrectly() {
        standing.recordResult(3, 1);
        assertThat(standing.getGoalDifference()).isEqualTo(2);

        standing.recordResult(0, 2);
        assertThat(standing.getGoalDifference()).isZero();
    }

    @Test
    @DisplayName("form should track last 5 results")
    void formShouldTrackLast5Results() {
        standing.recordResult(2, 0); // W
        standing.recordResult(1, 1); // D
        standing.recordResult(0, 1); // L
        standing.recordResult(3, 0); // W
        standing.recordResult(2, 1); // W

        assertThat(standing.getFormString()).isEqualTo("WDLWW");

        // Add one more - should drop first
        standing.recordResult(0, 0); // D
        assertThat(standing.getFormString()).isEqualTo("DLWWD");
    }

    @Test
    @DisplayName("compareTo should sort by points descending")
    void compareToShouldSortByPointsDescending() {
        Standing standing2 = new Standing(new Team("team2", "Team Two", "T2", "/badge.png",
                new TeamStrength(80, 80, 80, 80), List.of()));

        standing.recordResult(2, 0); // 3 points
        standing2.recordResult(1, 1); // 1 point

        List<Standing> standings = new ArrayList<>(List.of(standing2, standing));
        Collections.sort(standings);

        assertThat(standings.get(0).getTeamId()).isEqualTo("team1");
        assertThat(standings.get(1).getTeamId()).isEqualTo("team2");
    }

    @Test
    @DisplayName("compareTo should use goal difference as tiebreaker")
    void compareToShouldUseGoalDifferenceAsTiebreaker() {
        Standing standing2 = new Standing(new Team("team2", "Team Two", "T2", "/badge.png",
                new TeamStrength(80, 80, 80, 80), List.of()));

        standing.recordResult(3, 0); // 3 points, +3 GD
        standing2.recordResult(1, 0); // 3 points, +1 GD

        List<Standing> standings = new ArrayList<>(List.of(standing2, standing));
        Collections.sort(standings);

        assertThat(standings.get(0).getTeamId()).isEqualTo("team1");
    }

    @Test
    @DisplayName("createWithValues should create standing with all values")
    void createWithValuesShouldCreateStandingWithAllValues() {
        Standing s = Standing.createWithValues("t1", "Team", "/badge.png",
                10, 6, 2, 2, 20, 10, List.of('W', 'W', 'D', 'L', 'W'));

        assertThat(s.getPlayed()).isEqualTo(10);
        assertThat(s.getWon()).isEqualTo(6);
        assertThat(s.getDrawn()).isEqualTo(2);
        assertThat(s.getLost()).isEqualTo(2);
        assertThat(s.getGoalsFor()).isEqualTo(20);
        assertThat(s.getGoalsAgainst()).isEqualTo(10);
        assertThat(s.getFormString()).isEqualTo("WWDLW");
    }

    @Test
    @DisplayName("setPosition should update position")
    void setPositionShouldUpdatePosition() {
        standing.setPosition(5);
        assertThat(standing.getPosition()).isEqualTo(5);
    }
}

