package com.example.footballseasonsimulator.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FootballEventType enum.
 */
class FootballEventTypeTest {

    @Test
    @DisplayName("GOAL should be significant")
    void goalShouldBeSignificant() {
        assertThat(FootballEventType.GOAL.isSignificant()).isTrue();
    }

    @Test
    @DisplayName("SHOT_ON_TARGET should not be significant")
    void shotOnTargetShouldNotBeSignificant() {
        assertThat(FootballEventType.SHOT_ON_TARGET.isSignificant()).isFalse();
    }

    @Test
    @DisplayName("isGoal should return true for goal types")
    void isGoalShouldReturnTrueForGoalTypes() {
        assertThat(FootballEventType.GOAL.isGoal()).isTrue();
        assertThat(FootballEventType.OWN_GOAL.isGoal()).isTrue();
        assertThat(FootballEventType.PENALTY_SCORED.isGoal()).isTrue();
    }

    @Test
    @DisplayName("isGoal should return false for non-goal types")
    void isGoalShouldReturnFalseForNonGoalTypes() {
        assertThat(FootballEventType.PENALTY_MISSED.isGoal()).isFalse();
        assertThat(FootballEventType.SHOT_ON_TARGET.isGoal()).isFalse();
        assertThat(FootballEventType.YELLOW_CARD.isGoal()).isFalse();
    }

    @Test
    @DisplayName("isCard should return true for card types")
    void isCardShouldReturnTrueForCardTypes() {
        assertThat(FootballEventType.YELLOW_CARD.isCard()).isTrue();
        assertThat(FootballEventType.SECOND_YELLOW.isCard()).isTrue();
        assertThat(FootballEventType.RED_CARD.isCard()).isTrue();
    }

    @Test
    @DisplayName("isCard should return false for non-card types")
    void isCardShouldReturnFalseForNonCardTypes() {
        assertThat(FootballEventType.GOAL.isCard()).isFalse();
        assertThat(FootballEventType.FOUL.isCard()).isFalse();
    }

    @Test
    @DisplayName("getDisplayName should return correct names")
    void getDisplayNameShouldReturnCorrectNames() {
        assertThat(FootballEventType.GOAL.getDisplayName()).isEqualTo("Goal");
        assertThat(FootballEventType.YELLOW_CARD.getDisplayName()).isEqualTo("Yellow Card");
        assertThat(FootballEventType.HALF_TIME.getDisplayName()).isEqualTo("Half Time");
    }

    @Test
    @DisplayName("All event types should have display names")
    void allEventTypesShouldHaveDisplayNames() {
        for (FootballEventType type : FootballEventType.values()) {
            assertThat(type.getDisplayName()).isNotBlank();
        }
    }
}

