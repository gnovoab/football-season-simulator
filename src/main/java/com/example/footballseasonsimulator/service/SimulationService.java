package com.example.footballseasonsimulator.service;

import com.example.footballseasonsimulator.engine.MatchEngine;
import com.example.footballseasonsimulator.engine.RoundRobinScheduler;
import com.example.footballseasonsimulator.model.*;
import com.example.footballseasonsimulator.websocket.MatchEventPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Main service that orchestrates the football season simulation.
 */
@Service
public class SimulationService {
    
    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);
    
    private static final int FIXTURE_GAP_SECONDS = 10;
    private static final int SEASON_GAP_SECONDS = 60;
    private static final int COUNTDOWN_SECONDS = 30;
    
    private final LeagueDataService leagueDataService;
    private final StandingsService standingsService;
    private final MatchEventPublisher eventPublisher;
    private final RoundRobinScheduler scheduler;
    
    private final ScheduledExecutorService executor;
    private final Map<String, LeagueSimulation> leagueSimulations = new ConcurrentHashMap<>();
    
    public SimulationService(LeagueDataService leagueDataService, 
                            StandingsService standingsService,
                            MatchEventPublisher eventPublisher) {
        this.leagueDataService = leagueDataService;
        this.standingsService = standingsService;
        this.eventPublisher = eventPublisher;
        this.scheduler = new RoundRobinScheduler();
        this.executor = Executors.newScheduledThreadPool(10);
    }
    
    @PostConstruct
    public void init() {
        log.info("Starting Football Season Simulator...");
        
        // Initialize all leagues
        for (League league : leagueDataService.getAllLeagues()) {
            startLeagueSimulation(league);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Football Season Simulator...");
        executor.shutdown();
    }
    
    private void startLeagueSimulation(League league) {
        LeagueSimulation sim = new LeagueSimulation(league);
        leagueSimulations.put(league.id(), sim);
        sim.initializeSeason();
        sim.scheduleNextFixture(0);
    }
    
    // Public API methods
    
    public List<League> getAllLeagues() {
        return leagueDataService.getAllLeagues();
    }
    
    public League getLeague(String leagueId) {
        return leagueDataService.getLeague(leagueId);
    }
    
    public List<Standing> getStandings(String leagueId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        if (sim == null) return Collections.emptyList();

        // If there's an active fixture, return live standings with current match scores
        if (sim.currentFixture != null && sim.state == SeasonState.RUNNING_FIXTURE) {
            return sim.calculateLiveStandings();
        }

        return standingsService.getStandings(leagueId, sim.currentSeason);
    }

    public Standing getTeamStanding(String leagueId, String teamId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        if (sim == null) return null;
        return standingsService.getTeamStanding(leagueId, sim.currentSeason, teamId);
    }

    public Fixture getCurrentFixture(String leagueId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        return sim != null ? sim.currentFixture : null;
    }

    public Fixture getNextFixture(String leagueId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        if (sim == null || sim.seasonFixtures == null) return null;
        // currentMatchweek is 1-based after incrementing, so it points to the next fixture index
        int nextIndex = sim.currentMatchweek;
        if (nextIndex < sim.seasonFixtures.size()) {
            return sim.seasonFixtures.get(nextIndex);
        }
        return null;
    }

    public List<Match> getLiveMatches(String leagueId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        if (sim == null || sim.currentFixture == null) return Collections.emptyList();
        return sim.currentFixture.matches().stream()
            .filter(Match::isLive)
            .toList();
    }
    
    public List<Match> getCompletedMatches(String leagueId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        if (sim == null) return Collections.emptyList();
        return sim.completedMatches;
    }
    
    public Match getMatch(String matchId) {
        for (LeagueSimulation sim : leagueSimulations.values()) {
            for (Match match : sim.completedMatches) {
                if (match.getId().equals(matchId)) return match;
            }
            if (sim.currentFixture != null) {
                for (Match match : sim.currentFixture.matches()) {
                    if (match.getId().equals(matchId)) return match;
                }
            }
        }
        return null;
    }
    
    public SimulationStatus getStatus(String leagueId) {
        LeagueSimulation sim = leagueSimulations.get(leagueId);
        if (sim == null) return null;
        return new SimulationStatus(
            sim.league.id(),
            sim.league.name(),
            sim.state,
            sim.currentSeason,
            sim.currentMatchweek,
            sim.league.totalMatchweeks()
        );
    }
    
    public record SimulationStatus(
        String leagueId,
        String leagueName,
        SeasonState state,
        int season,
        int currentMatchweek,
        int totalMatchweeks
    ) {}
    
    // Inner class for per-league simulation state
    private class LeagueSimulation {
        final League league;
        int currentSeason = 1;
        int currentMatchweek = 0;
        List<Fixture> seasonFixtures;
        Fixture currentFixture;
        SeasonState state = SeasonState.IDLE;
        List<MatchEngine> activeEngines = new ArrayList<>();
        List<Match> completedMatches = new ArrayList<>();
        ScheduledFuture<?> tickerTask;
        
        LeagueSimulation(League league) {
            this.league = league;
        }
        
        void initializeSeason() {
            log.info("Initializing season {} for {}", currentSeason, league.name());
            seasonFixtures = scheduler.generateSeasonFixtures(league, currentSeason);
            currentMatchweek = 0;
            completedMatches.clear();
            standingsService.initializeSeason(league, currentSeason);
            updateState(SeasonState.WAITING_NEXT_FIXTURE);
        }
        
        void scheduleNextFixture(int delaySeconds) {
            executor.schedule(this::startCountdown, delaySeconds, TimeUnit.SECONDS);
        }

        void startCountdown() {
            if (currentMatchweek >= seasonFixtures.size()) {
                completeSeason();
                return;
            }

            // Get the upcoming fixture for countdown display
            Fixture upcomingFixture = seasonFixtures.get(currentMatchweek);
            int upcomingMatchweek = currentMatchweek + 1;

            log.info("[{}] Starting countdown for matchweek {}/{}", league.name(), upcomingMatchweek, seasonFixtures.size());
            updateState(SeasonState.COUNTDOWN);

            // Run countdown from COUNTDOWN_SECONDS to 0
            for (int i = COUNTDOWN_SECONDS; i >= 0; i--) {
                final int secondsRemaining = i;
                executor.schedule(() -> {
                    eventPublisher.publishCountdown(league.id(), upcomingMatchweek, secondsRemaining, upcomingFixture);
                    if (secondsRemaining == 0) {
                        runNextFixture();
                    }
                }, COUNTDOWN_SECONDS - i, TimeUnit.SECONDS);
            }
        }

        void runNextFixture() {
            if (currentMatchweek >= seasonFixtures.size()) {
                completeSeason();
                return;
            }

            currentFixture = seasonFixtures.get(currentMatchweek);
            currentMatchweek++;

            log.info("[{}] Starting matchweek {}/{}", league.name(), currentMatchweek, seasonFixtures.size());
            updateState(SeasonState.RUNNING_FIXTURE);
            eventPublisher.publishFixtureStart(currentFixture);

            activeEngines.clear();
            for (Match match : currentFixture.matches()) {
                MatchEngine engine = new MatchEngine();
                engine.setEventCallback(e -> {
                    eventPublisher.publishEvent(league.id(), e);
                    // Publish live standings when a goal is scored
                    if (e.type().isGoal()) {
                        publishLiveStandings();
                    }
                });
                engine.setStateCallback(m -> eventPublisher.publishMatchState(m));
                engine.startMatch(match);
                activeEngines.add(engine);
            }

            tickerTask = executor.scheduleAtFixedRate(
                this::tickAllMatches, 0, MatchEngine.getTickIntervalMs(), TimeUnit.MILLISECONDS);
        }
        
        void tickAllMatches() {
            boolean anyRunning = false;
            for (MatchEngine engine : activeEngines) {
                if (engine.tick()) anyRunning = true;
            }
            
            if (!anyRunning) {
                if (tickerTask != null) {
                    tickerTask.cancel(false);
                    tickerTask = null;
                }
                onFixtureComplete();
            }
        }
        
        void onFixtureComplete() {
            log.info("[{}] Matchweek {} complete", league.name(), currentMatchweek);
            
            for (Match match : currentFixture.matches()) {
                standingsService.updateFromMatch(league.id(), currentSeason, match);
                completedMatches.add(match);
            }
            
            List<Standing> standings = standingsService.getStandings(league.id(), currentSeason);
            eventPublisher.publishStandings(league.id(), currentSeason, standings);
            
            if (currentMatchweek >= seasonFixtures.size()) {
                completeSeason();
            } else {
                updateState(SeasonState.WAITING_NEXT_FIXTURE);
                scheduleNextFixture(FIXTURE_GAP_SECONDS);
            }
        }
        
        void completeSeason() {
            log.info("[{}] Season {} complete!", league.name(), currentSeason);
            updateState(SeasonState.SEASON_COMPLETE);
            
            currentSeason++;
            executor.schedule(() -> {
                initializeSeason();
                scheduleNextFixture(FIXTURE_GAP_SECONDS);
            }, SEASON_GAP_SECONDS, TimeUnit.SECONDS);
            
            updateState(SeasonState.WAITING_NEXT_SEASON);
        }
        
        void updateState(SeasonState newState) {
            this.state = newState;
            eventPublisher.publishSeasonState(league.id(), newState, currentSeason, currentMatchweek);
        }

        /**
         * Calculate live standings based on current match scores.
         * This creates a temporary view of standings without modifying the actual standings.
         */
        List<Standing> calculateLiveStandings() {
            // Get current standings as base
            List<Standing> baseStandings = standingsService.getStandings(league.id(), currentSeason);

            // Create a map of team standings - copy base values
            Map<String, LiveStandingData> liveData = new HashMap<>();
            for (Standing s : baseStandings) {
                liveData.put(s.getTeamId(), new LiveStandingData(s));
            }

            // Apply current match scores (these are live matches not yet recorded in standings)
            if (currentFixture != null) {
                for (Match match : currentFixture.matches()) {
                    LiveStandingData homeData = liveData.get(match.getHomeTeam().id());
                    LiveStandingData awayData = liveData.get(match.getAwayTeam().id());

                    if (homeData != null && awayData != null) {
                        int homeScore = match.getHomeScore();
                        int awayScore = match.getAwayScore();

                        // Only add live match data if match is in progress or finished this matchweek
                        // The base standings don't include current matchweek results yet
                        homeData.goalsFor += homeScore;
                        homeData.goalsAgainst += awayScore;
                        awayData.goalsFor += awayScore;
                        awayData.goalsAgainst += homeScore;

                        // Add this match to played count
                        homeData.played++;
                        awayData.played++;

                        // Determine current result
                        if (homeScore > awayScore) {
                            homeData.won++;
                            awayData.lost++;
                        } else if (awayScore > homeScore) {
                            awayData.won++;
                            homeData.lost++;
                        } else {
                            homeData.drawn++;
                            awayData.drawn++;
                        }
                    }
                }
            }

            // Convert to Standing objects and sort
            List<Standing> liveStandings = new ArrayList<>();
            for (LiveStandingData data : liveData.values()) {
                liveStandings.add(data.toStanding());
            }
            liveStandings.sort(null);

            // Set positions
            for (int i = 0; i < liveStandings.size(); i++) {
                liveStandings.get(i).setPosition(i + 1);
            }

            return liveStandings;
        }

        /**
         * Calculate and publish live standings based on current match scores.
         */
        void publishLiveStandings() {
            List<Standing> liveStandings = calculateLiveStandings();
            eventPublisher.publishStandings(league.id(), currentSeason, liveStandings);
        }
    }

    /**
     * Helper class to calculate live standings.
     */
    private static class LiveStandingData {
        final String teamId;
        final String teamName;
        final String teamBadgeUrl;
        int played;
        int won;
        int drawn;
        int lost;
        int goalsFor;
        int goalsAgainst;
        final List<Character> form;

        LiveStandingData(Standing base) {
            this.teamId = base.getTeamId();
            this.teamName = base.getTeamName();
            this.teamBadgeUrl = base.getTeamBadgeUrl();
            this.played = base.getPlayed();
            this.won = base.getWon();
            this.drawn = base.getDrawn();
            this.lost = base.getLost();
            this.goalsFor = base.getGoalsFor();
            this.goalsAgainst = base.getGoalsAgainst();
            this.form = new ArrayList<>(base.getForm());
        }

        Standing toStanding() {
            return Standing.createWithValues(teamId, teamName, teamBadgeUrl,
                    played, won, drawn, lost, goalsFor, goalsAgainst, form);
        }
    }
}

