package com.example.footballseasonsimulator.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MatchPhase enum.
 */
class MatchPhaseTest {

    @Test
    @DisplayName("isPlaying should return true for FIRST_HALF and SECOND_HALF")
    void isPlayingShouldReturnTrueForPlayingPhases() {
        assertThat(MatchPhase.FIRST_HALF.isPlaying()).isTrue();
        assertThat(MatchPhase.SECOND_HALF.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("isPlaying should return false for non-playing phases")
    void isPlayingShouldReturnFalseForNonPlayingPhases() {
        assertThat(MatchPhase.NOT_STARTED.isPlaying()).isFalse();
        assertThat(MatchPhase.HALF_TIME.isPlaying()).isFalse();
        assertThat(MatchPhase.FULL_TIME.isPlaying()).isFalse();
    }

    @Test
    @DisplayName("isFinished should return true only for FULL_TIME")
    void isFinishedShouldReturnTrueOnlyForFullTime() {
        assertThat(MatchPhase.FULL_TIME.isFinished()).isTrue();
        assertThat(MatchPhase.NOT_STARTED.isFinished()).isFalse();
        assertThat(MatchPhase.FIRST_HALF.isFinished()).isFalse();
        assertThat(MatchPhase.HALF_TIME.isFinished()).isFalse();
        assertThat(MatchPhase.SECOND_HALF.isFinished()).isFalse();
    }

    @Test
    @DisplayName("getDisplayName should return correct names")
    void getDisplayNameShouldReturnCorrectNames() {
        assertThat(MatchPhase.NOT_STARTED.getDisplayName()).isEqualTo("Not Started");
        assertThat(MatchPhase.FIRST_HALF.getDisplayName()).isEqualTo("First Half");
        assertThat(MatchPhase.HALF_TIME.getDisplayName()).isEqualTo("Half Time");
        assertThat(MatchPhase.SECOND_HALF.getDisplayName()).isEqualTo("Second Half");
        assertThat(MatchPhase.FULL_TIME.getDisplayName()).isEqualTo("Full Time");
    }

    @Test
    @DisplayName("All phases should have display names")
    void allPhasesShouldHaveDisplayNames() {
        for (MatchPhase phase : MatchPhase.values()) {
            assertThat(phase.getDisplayName()).isNotBlank();
        }
    }
}

