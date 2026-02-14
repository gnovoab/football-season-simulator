package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages the continuous simulation of football seasons.
 * Runs all matches in a fixture simultaneously, then waits before the next fixture.
 */
public class SeasonRunner {
    
    private static final Logger log = LoggerFactory.getLogger(SeasonRunner.class);
    
    private static final int FIXTURE_GAP_SECONDS = 30;  // 30 seconds between fixtures
    private static final int SEASON_GAP_SECONDS = 60;   // 60 seconds between seasons
    
    private final RoundRobinScheduler scheduler;
    private final Map<String, List<MatchEngine>> activeEngines;
    private final ScheduledExecutorService executor;
    
    private League league;
    private int currentSeason;
    private int currentMatchweek;
    private List<Fixture> seasonFixtures;
    private Fixture currentFixture;
    private SeasonState state;
    
    // Standings for current season
    private Map<String, Standing> standings;

    // Ticker task handle
    private ScheduledFuture<?> tickerTask;

    // Callbacks
    private Consumer<MatchEvent> eventCallback;
    private Consumer<Match> matchStateCallback;
    private Consumer<SeasonState> stateCallback;
    private Consumer<List<Standing>> standingsCallback;
    private Consumer<Fixture> fixtureCallback;
    
    public SeasonRunner() {
        this.scheduler = new RoundRobinScheduler();
        this.activeEngines = new HashMap<>();
        this.executor = Executors.newScheduledThreadPool(2);
        this.state = SeasonState.IDLE;
        this.currentSeason = 1;
        this.currentMatchweek = 0;
    }
    
    public void setEventCallback(Consumer<MatchEvent> callback) {
        this.eventCallback = callback;
    }
    
    public void setMatchStateCallback(Consumer<Match> callback) {
        this.matchStateCallback = callback;
    }
    
    public void setStateCallback(Consumer<SeasonState> callback) {
        this.stateCallback = callback;
    }
    
    public void setStandingsCallback(Consumer<List<Standing>> callback) {
        this.standingsCallback = callback;
    }
    
    public void setFixtureCallback(Consumer<Fixture> callback) {
        this.fixtureCallback = callback;
    }
    
    /**
     * Initialize and start running seasons for a league.
     */
    public void start(League league) {
        this.league = league;
        initializeSeason();
        scheduleNextFixture(0);
    }
    
    private void initializeSeason() {
        log.info("Initializing season {} for {}", currentSeason, league.name());
        
        // Generate fixtures for the season
        seasonFixtures = scheduler.generateSeasonFixtures(league, currentSeason);
        currentMatchweek = 0;
        
        // Initialize standings
        standings = new HashMap<>();
        for (Team team : league.teams()) {
            standings.put(team.id(), new Standing(team));
        }
        
        updateState(SeasonState.WAITING_NEXT_FIXTURE);
        publishStandings();
    }
    
    private void scheduleNextFixture(int delaySeconds) {
        executor.schedule(this::runNextFixture, delaySeconds, TimeUnit.SECONDS);
    }
    
    private void runNextFixture() {
        if (currentMatchweek >= seasonFixtures.size()) {
            // Season complete
            completeSeason();
            return;
        }
        
        currentFixture = seasonFixtures.get(currentMatchweek);
        currentMatchweek++;
        
        log.info("Starting matchweek {} of season {}", currentMatchweek, currentSeason);
        updateState(SeasonState.RUNNING_FIXTURE);
        
        if (fixtureCallback != null) {
            fixtureCallback.accept(currentFixture);
        }
        
        // Create engines for all matches in this fixture
        List<MatchEngine> engines = new ArrayList<>();
        for (Match match : currentFixture.matches()) {
            MatchEngine engine = new MatchEngine();
            engine.setEventCallback(this::onMatchEvent);
            engine.setStateCallback(this::onMatchStateChange);
            engine.startMatch(match);
            engines.add(engine);
        }
        activeEngines.put(league.id(), engines);
        
        // Start ticking all matches
        tickerTask = executor.scheduleAtFixedRate(
            this::tickAllMatches,
            0,
            MatchEngine.getTickIntervalMs(),
            TimeUnit.MILLISECONDS
        );
    }
    
    private void tickAllMatches() {
        List<MatchEngine> engines = activeEngines.get(league.id());
        if (engines == null || engines.isEmpty()) {
            return;
        }
        
        boolean anyRunning = false;
        for (MatchEngine engine : engines) {
            if (engine.tick()) {
                anyRunning = true;
            }
        }
        
        if (!anyRunning) {
            // Stop the ticker task
            if (tickerTask != null) {
                tickerTask.cancel(false);
                tickerTask = null;
            }
            // All matches finished
            onFixtureComplete();
        }
    }
    
    private void onFixtureComplete() {
        log.info("Matchweek {} complete", currentMatchweek);
        
        // Update standings
        for (Match match : currentFixture.matches()) {
            Standing homeStanding = standings.get(match.getHomeTeam().id());
            Standing awayStanding = standings.get(match.getAwayTeam().id());
            
            if (homeStanding != null) {
                homeStanding.recordResult(match.getHomeScore(), match.getAwayScore());
            }
            if (awayStanding != null) {
                awayStanding.recordResult(match.getAwayScore(), match.getHomeScore());
            }
        }
        
        // Sort and assign positions
        List<Standing> sortedStandings = new ArrayList<>(standings.values());
        sortedStandings.sort(null);
        for (int i = 0; i < sortedStandings.size(); i++) {
            sortedStandings.get(i).setPosition(i + 1);
        }
        
        publishStandings();
        activeEngines.remove(league.id());
        
        // Schedule next fixture or season
        if (currentMatchweek >= seasonFixtures.size()) {
            completeSeason();
        } else {
            updateState(SeasonState.WAITING_NEXT_FIXTURE);
            scheduleNextFixture(FIXTURE_GAP_SECONDS);
        }
    }
    
    private void completeSeason() {
        log.info("Season {} complete!", currentSeason);
        updateState(SeasonState.SEASON_COMPLETE);
        
        // Start next season after delay
        currentSeason++;
        executor.schedule(() -> {
            initializeSeason();
            scheduleNextFixture(FIXTURE_GAP_SECONDS);
        }, SEASON_GAP_SECONDS, TimeUnit.SECONDS);
        
        updateState(SeasonState.WAITING_NEXT_SEASON);
    }
    
    private void onMatchEvent(MatchEvent event) {
        if (eventCallback != null) {
            eventCallback.accept(event);
        }
    }
    
    private void onMatchStateChange(Match match) {
        if (matchStateCallback != null) {
            matchStateCallback.accept(match);
        }
    }
    
    private void updateState(SeasonState newState) {
        this.state = newState;
        if (stateCallback != null) {
            stateCallback.accept(newState);
        }
    }
    
    private void publishStandings() {
        if (standingsCallback != null) {
            List<Standing> sortedStandings = new ArrayList<>(standings.values());
            sortedStandings.sort(null);
            standingsCallback.accept(sortedStandings);
        }
    }
    
    public void stop() {
        executor.shutdown();
    }
    
    // Getters
    public SeasonState getState() { return state; }
    public int getCurrentSeason() { return currentSeason; }
    public int getCurrentMatchweek() { return currentMatchweek; }
    public Fixture getCurrentFixture() { return currentFixture; }
    public List<Standing> getStandings() {
        List<Standing> sorted = new ArrayList<>(standings.values());
        sorted.sort(null);
        return sorted;
    }
}

