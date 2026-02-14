package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Engine that simulates a football match in compressed time.
 * 
 * Time compression: 90 minutes → ~150 seconds (2.5 minutes)
 * This means each real second represents ~36 match seconds.
 * 
 * The engine ticks every 250ms, advancing match time accordingly.
 */
public class MatchEngine {
    
    private static final Logger log = LoggerFactory.getLogger(MatchEngine.class);
    
    // Time configuration
    private static final int MATCH_DURATION_MINUTES = 90;
    private static final int HALF_TIME_MINUTE = 45;
    private static final int REAL_MATCH_DURATION_MS = 150_000;  // 150 seconds = 2.5 minutes
    private static final int TICK_INTERVAL_MS = 250;            // Tick every 250ms
    
    // Calculated: how many match minutes per tick
    private static final double MINUTES_PER_TICK = 
        (double) MATCH_DURATION_MINUTES / (REAL_MATCH_DURATION_MS / TICK_INTERVAL_MS);
    
    private final EventGenerator eventGenerator;
    private final Random random = new Random();
    
    private Match currentMatch;
    private double accumulatedMinutes;
    private int lastProcessedMinute;
    private int stoppageTimeFirstHalf;
    private int stoppageTimeSecondHalf;
    private boolean running;
    
    private Consumer<MatchEvent> eventCallback;
    private Consumer<Match> stateCallback;
    
    public MatchEngine() {
        this.eventGenerator = new EventGenerator();
    }
    
    /**
     * Set callback for match events.
     */
    public void setEventCallback(Consumer<MatchEvent> callback) {
        this.eventCallback = callback;
    }
    
    /**
     * Set callback for match state changes.
     */
    public void setStateCallback(Consumer<Match> callback) {
        this.stateCallback = callback;
    }
    
    /**
     * Start simulating a match.
     */
    public void startMatch(Match match) {
        this.currentMatch = match;
        this.accumulatedMinutes = 0;
        this.lastProcessedMinute = 0;
        this.stoppageTimeFirstHalf = random.nextInt(3) + 1;  // 1-3 minutes
        this.stoppageTimeSecondHalf = random.nextInt(4) + 2; // 2-5 minutes
        this.running = true;
        
        // Kick off
        match.setPhase(MatchPhase.FIRST_HALF);
        match.setMatchMinute(1);
        
        MatchEvent kickOff = MatchEvent.simple(1, FootballEventType.KICK_OFF, "Match kicks off!");
        match.addEvent(kickOff);
        publishEvent(kickOff);
        publishState();
        
        log.info("Match started: {} vs {}", match.getHomeTeam().name(), match.getAwayTeam().name());
    }
    
    /**
     * Process one tick of the match simulation.
     * Should be called every TICK_INTERVAL_MS milliseconds.
     * 
     * @return true if match is still running, false if finished
     */
    public boolean tick() {
        if (!running || currentMatch == null) {
            return false;
        }
        
        accumulatedMinutes += MINUTES_PER_TICK;
        int currentMinute = (int) accumulatedMinutes;
        
        // Process each minute that has passed
        while (lastProcessedMinute < currentMinute && running) {
            lastProcessedMinute++;
            processMinute(lastProcessedMinute);
        }
        
        // Update match time display
        updateMatchTime();
        publishState();
        
        return running;
    }
    
    private void processMinute(int minute) {
        MatchPhase phase = currentMatch.getPhase();
        
        // Handle phase transitions
        if (phase == MatchPhase.FIRST_HALF) {
            if (minute > HALF_TIME_MINUTE + stoppageTimeFirstHalf) {
                transitionToHalfTime();
                return;
            }
        } else if (phase == MatchPhase.SECOND_HALF) {
            if (minute > MATCH_DURATION_MINUTES + stoppageTimeSecondHalf) {
                transitionToFullTime();
                return;
            }
        }
        
        // Generate events for this minute
        if (phase.isPlaying()) {
            int displayMinute = phase == MatchPhase.SECOND_HALF && minute > MATCH_DURATION_MINUTES 
                ? MATCH_DURATION_MINUTES : minute;
            int additional = phase == MatchPhase.FIRST_HALF && minute > HALF_TIME_MINUTE 
                ? minute - HALF_TIME_MINUTE 
                : (phase == MatchPhase.SECOND_HALF && minute > MATCH_DURATION_MINUTES 
                    ? minute - MATCH_DURATION_MINUTES : 0);
            
            List<MatchEvent> events = eventGenerator.generateEventsForMinute(
                currentMatch, displayMinute, additional);
            
            for (MatchEvent event : events) {
                currentMatch.addEvent(event);
                publishEvent(event);
            }
        }
    }
    
    private void transitionToHalfTime() {
        currentMatch.setPhase(MatchPhase.HALF_TIME);
        MatchEvent htEvent = MatchEvent.simple(45, FootballEventType.HALF_TIME, 
            "Half Time: " + currentMatch.getScoreDisplay());
        currentMatch.addEvent(htEvent);
        publishEvent(htEvent);
        
        // Brief pause then start second half
        currentMatch.setPhase(MatchPhase.SECOND_HALF);
        MatchEvent kickOff = MatchEvent.simple(46, FootballEventType.SECOND_HALF_KICK_OFF, 
            "Second half begins!");
        currentMatch.addEvent(kickOff);
        publishEvent(kickOff);
    }
    
    private void transitionToFullTime() {
        currentMatch.setPhase(MatchPhase.FULL_TIME);
        running = false;
        
        MatchEvent ftEvent = MatchEvent.simple(90, FootballEventType.FULL_TIME,
            "Full Time: " + currentMatch.getHomeTeam().shortName() + " " + 
            currentMatch.getScoreDisplay() + " " + currentMatch.getAwayTeam().shortName());
        currentMatch.addEvent(ftEvent);
        publishEvent(ftEvent);
        
        log.info("Match finished: {} {} - {} {}", 
            currentMatch.getHomeTeam().name(), currentMatch.getHomeScore(),
            currentMatch.getAwayScore(), currentMatch.getAwayTeam().name());
    }
    
    private void updateMatchTime() {
        int minute = lastProcessedMinute;
        int additional = 0;
        
        if (currentMatch.getPhase() == MatchPhase.FIRST_HALF && minute > HALF_TIME_MINUTE) {
            additional = minute - HALF_TIME_MINUTE;
            minute = HALF_TIME_MINUTE;
        } else if (currentMatch.getPhase() == MatchPhase.SECOND_HALF && minute > MATCH_DURATION_MINUTES) {
            additional = minute - MATCH_DURATION_MINUTES;
            minute = MATCH_DURATION_MINUTES;
        }
        
        currentMatch.setMatchMinute(minute);
        currentMatch.setAdditionalMinutes(additional);
    }
    
    private void publishEvent(MatchEvent event) {
        if (eventCallback != null) {
            eventCallback.accept(event);
        }
    }
    
    private void publishState() {
        if (stateCallback != null) {
            stateCallback.accept(currentMatch);
        }
    }
    
    public Match getCurrentMatch() {
        return currentMatch;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public static int getTickIntervalMs() {
        return TICK_INTERVAL_MS;
    }
}

