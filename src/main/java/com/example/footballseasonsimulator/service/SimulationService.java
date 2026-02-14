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
    
    private static final int FIXTURE_GAP_SECONDS = 30;
    private static final int SEASON_GAP_SECONDS = 60;
    
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
        return standingsService.getStandings(leagueId, sim.currentSeason);
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
            executor.schedule(this::runNextFixture, delaySeconds, TimeUnit.SECONDS);
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
                engine.setEventCallback(e -> eventPublisher.publishEvent(league.id(), e));
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
    }
}

