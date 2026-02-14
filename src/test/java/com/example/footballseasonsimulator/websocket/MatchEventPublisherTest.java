package com.example.footballseasonsimulator.websocket;

import com.example.footballseasonsimulator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
class MatchEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MatchEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new MatchEventPublisher(messagingTemplate);
    }

    @Test
    @DisplayName("publishEvent should send to league-specific and global topics")
    void publishEventShouldSendToTopics() {
        MatchEvent event = MatchEvent.simple(45, FootballEventType.GOAL, "Goal scored!");

        publisher.publishEvent("premier-league", event);

        verify(messagingTemplate).convertAndSend("/topic/league/premier-league/events", event);
        verify(messagingTemplate).convertAndSend("/topic/events", event);
    }

    @Test
    @DisplayName("publishMatchState should send MatchStateDTO to topics")
    void publishMatchStateShouldSendDTO() {
        Match match = createTestMatch();

        publisher.publishMatchState(match);

        verify(messagingTemplate).convertAndSend(eq("/topic/match/" + match.getId()), any(MatchEventPublisher.MatchStateDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/league/premier-league/matches"), any(MatchEventPublisher.MatchStateDTO.class));
    }

    @Test
    @DisplayName("publishStandings should send StandingsDTO to league topic")
    void publishStandingsShouldSendDTO() {
        Team team = new Team("team1", "Team One", "T1", "/badge.png",
                new TeamStrength(80, 80, 80, 80), List.of());
        List<Standing> standings = List.of(new Standing(team));

        publisher.publishStandings("premier-league", 2025, standings);

        ArgumentCaptor<MatchEventPublisher.StandingsDTO> captor = ArgumentCaptor.forClass(MatchEventPublisher.StandingsDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/league/premier-league/standings"), captor.capture());

        MatchEventPublisher.StandingsDTO dto = captor.getValue();
        assertThat(dto.leagueId()).isEqualTo("premier-league");
        assertThat(dto.season()).isEqualTo(2025);
        assertThat(dto.standings()).hasSize(1);
    }

    @Test
    @DisplayName("publishFixtureStart should send fixture to league topic")
    void publishFixtureStartShouldSendFixture() {
        Fixture fixture = new Fixture("premier-league", 2025, 1, List.of());

        publisher.publishFixtureStart(fixture);

        verify(messagingTemplate).convertAndSend("/topic/league/premier-league/fixture", fixture);
    }

    @Test
    @DisplayName("publishSeasonState should send SeasonStateDTO to league topic")
    void publishSeasonStateShouldSendDTO() {
        publisher.publishSeasonState("premier-league", SeasonState.RUNNING_FIXTURE, 2025, 15);

        ArgumentCaptor<MatchEventPublisher.SeasonStateDTO> captor = ArgumentCaptor.forClass(MatchEventPublisher.SeasonStateDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/league/premier-league/state"), captor.capture());

        MatchEventPublisher.SeasonStateDTO dto = captor.getValue();
        assertThat(dto.leagueId()).isEqualTo("premier-league");
        assertThat(dto.state()).isEqualTo(SeasonState.RUNNING_FIXTURE);
        assertThat(dto.season()).isEqualTo(2025);
        assertThat(dto.matchweek()).isEqualTo(15);
    }

    @Test
    @DisplayName("publishCountdown should send CountdownDTO to league topic")
    void publishCountdownShouldSendDTO() {
        Fixture fixture = new Fixture("premier-league", 2025, 1, List.of());

        publisher.publishCountdown("premier-league", 1, 30, fixture);

        ArgumentCaptor<MatchEventPublisher.CountdownDTO> captor = ArgumentCaptor.forClass(MatchEventPublisher.CountdownDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/league/premier-league/countdown"), captor.capture());

        MatchEventPublisher.CountdownDTO dto = captor.getValue();
        assertThat(dto.leagueId()).isEqualTo("premier-league");
        assertThat(dto.matchweek()).isEqualTo(1);
        assertThat(dto.secondsRemaining()).isEqualTo(30);
        assertThat(dto.upcomingFixture()).isEqualTo(fixture);
    }

    @Test
    @DisplayName("MatchStateDTO should correctly map from Match")
    void matchStateDTOShouldMapFromMatch() {
        Match match = createTestMatch();
        match.setPhase(MatchPhase.SECOND_HALF);
        match.setMatchMinute(67);

        MatchEventPublisher.MatchStateDTO dto = new MatchEventPublisher.MatchStateDTO(match);

        assertThat(dto.matchId()).isEqualTo(match.getId());
        assertThat(dto.leagueId()).isEqualTo("premier-league");
        assertThat(dto.homeTeamId()).isEqualTo("team1");
        assertThat(dto.awayTeamId()).isEqualTo("team2");
        assertThat(dto.phase()).isEqualTo("SECOND_HALF");
    }

    private Match createTestMatch() {
        Team homeTeam = new Team("team1", "Team One", "T1", "/badge1.png",
                new TeamStrength(80, 80, 80, 80), List.of());
        Team awayTeam = new Team("team2", "Team Two", "T2", "/badge2.png",
                new TeamStrength(75, 75, 75, 75), List.of());
        return new Match("premier-league", 2025, 1, homeTeam, awayTeam);
    }
}

