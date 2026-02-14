package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates match events based on probabilities and team strengths.
 * Inspired by virtua-football's SituationPlanner with team strength modifiers.
 */
public class EventGenerator {
    
    private final Random random = new Random();
    
    // Base probabilities per match minute (tuned for ~2.5-3 goals per match average)
    private static final double BASE_SHOT_CHANCE = 0.035;      // ~3 shots per team per minute
    private static final double BASE_FOUL_CHANCE = 0.012;      // ~1 foul per minute
    private static final double BASE_CORNER_CHANCE = 0.008;    // ~0.7 corners per minute

    // Conversion rates (tuned for realistic goal output)
    private static final double SHOT_ON_TARGET_RATE = 0.40;    // 40% of shots on target
    private static final double GOAL_CONVERSION_RATE = 0.28;   // 28% of on-target shots are goals
    private static final double SAVE_RATE = 0.70;              // 70% of on-target non-goals are saves
    private static final double YELLOW_CARD_RATE = 0.10;       // 10% of fouls result in yellow
    private static final double RED_CARD_RATE = 0.005;         // 0.5% of fouls result in red
    private static final double PENALTY_RATE = 0.02;           // 2% of fouls in box = penalty
    private static final double PENALTY_CONVERSION = 0.78;     // 78% of penalties scored
    
    /**
     * Generate events for a single match minute.
     */
    public List<MatchEvent> generateEventsForMinute(Match match, int minute, int additionalMinutes) {
        List<MatchEvent> events = new ArrayList<>();
        
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        
        // Calculate attack probabilities based on team strengths
        double homeAttackMod = calculateAttackModifier(home, away);
        double awayAttackMod = calculateAttackModifier(away, home);
        
        // Home team events
        events.addAll(generateTeamEvents(match, home, away, minute, additionalMinutes, homeAttackMod, true));
        
        // Away team events
        events.addAll(generateTeamEvents(match, away, home, minute, additionalMinutes, awayAttackMod, false));
        
        return events;
    }
    
    private List<MatchEvent> generateTeamEvents(Match match, Team attacking, Team defending,
                                                 int minute, int additionalMinutes,
                                                 double attackMod, boolean isHome) {
        List<MatchEvent> events = new ArrayList<>();
        
        // Shot attempt
        if (random.nextDouble() < BASE_SHOT_CHANCE * attackMod) {
            events.addAll(generateShotSequence(attacking, defending, minute, additionalMinutes));
        }
        
        // Foul
        if (random.nextDouble() < BASE_FOUL_CHANCE) {
            events.addAll(generateFoulSequence(attacking, defending, minute, additionalMinutes));
        }
        
        // Corner
        if (random.nextDouble() < BASE_CORNER_CHANCE * attackMod) {
            Player taker = selectPlayer(attacking, Position.MIDFIELDER, Position.FORWARD);
            events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.CORNER_KICK,
                attacking, taker, taker.name() + " takes the corner"));
        }
        
        return events;
    }
    
    private List<MatchEvent> generateShotSequence(Team attacking, Team defending,
                                                   int minute, int additionalMinutes) {
        List<MatchEvent> events = new ArrayList<>();
        Player shooter = selectPlayer(attacking, Position.FORWARD, Position.MIDFIELDER);
        
        boolean onTarget = random.nextDouble() < SHOT_ON_TARGET_RATE;
        
        if (onTarget) {
            // Check for goal
            double goalChance = GOAL_CONVERSION_RATE * (attacking.strength().attack() / 80.0)
                              * (80.0 / defending.strength().goalkeeper());
            
            if (random.nextDouble() < goalChance) {
                events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.GOAL,
                    attacking, shooter, "GOAL! " + shooter.name() + " scores!"));
            } else {
                events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.SHOT_ON_TARGET,
                    attacking, shooter, shooter.name() + " shoots on target"));
                
                if (random.nextDouble() < SAVE_RATE) {
                    Player keeper = defending.getGoalkeeper();
                    if (keeper != null) {
                        events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.SAVE,
                            defending, keeper, "Save by " + keeper.name()));
                    }
                }
            }
        } else {
            events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.SHOT_OFF_TARGET,
                attacking, shooter, shooter.name() + " shoots wide"));
        }
        
        return events;
    }
    
    private List<MatchEvent> generateFoulSequence(Team foulingTeam, Team fouledTeam,
                                                   int minute, int additionalMinutes) {
        List<MatchEvent> events = new ArrayList<>();
        Player fouler = selectPlayer(foulingTeam, Position.DEFENDER, Position.MIDFIELDER);
        
        events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.FOUL,
            foulingTeam, fouler, "Foul by " + fouler.name()));
        
        // Check for card
        if (random.nextDouble() < RED_CARD_RATE) {
            events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.RED_CARD,
                foulingTeam, fouler, "RED CARD for " + fouler.name()));
        } else if (random.nextDouble() < YELLOW_CARD_RATE) {
            events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.YELLOW_CARD,
                foulingTeam, fouler, "Yellow card for " + fouler.name()));
        }
        
        // Check for penalty
        if (random.nextDouble() < PENALTY_RATE) {
            events.addAll(generatePenaltySequence(fouledTeam, foulingTeam, minute, additionalMinutes));
        }
        
        return events;
    }
    
    private List<MatchEvent> generatePenaltySequence(Team attacking, Team defending,
                                                      int minute, int additionalMinutes) {
        List<MatchEvent> events = new ArrayList<>();
        Player taker = selectPlayer(attacking, Position.FORWARD, Position.MIDFIELDER);
        Player keeper = defending.getGoalkeeper();
        
        events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.PENALTY_AWARDED,
            attacking, taker, "Penalty awarded!"));
        
        if (random.nextDouble() < PENALTY_CONVERSION) {
            events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.PENALTY_SCORED,
                attacking, taker, "GOAL! " + taker.name() + " converts the penalty!"));
        } else {
            if (random.nextBoolean() && keeper != null) {
                events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.PENALTY_SAVED,
                    defending, keeper, "Penalty saved by " + keeper.name() + "!"));
            } else {
                events.add(MatchEvent.withPlayer(minute, additionalMinutes, FootballEventType.PENALTY_MISSED,
                    attacking, taker, taker.name() + " misses the penalty!"));
            }
        }
        
        return events;
    }
    
    private double calculateAttackModifier(Team attacking, Team defending) {
        double attackStrength = attacking.strength().attack() / 100.0;
        double midfieldStrength = attacking.strength().midfield() / 100.0;
        double defenseWeakness = 1.0 - (defending.strength().defense() / 100.0);
        
        return 0.5 + (attackStrength * 0.25) + (midfieldStrength * 0.15) + (defenseWeakness * 0.1);
    }
    
    private Player selectPlayer(Team team, Position primary, Position secondary) {
        List<Player> candidates = team.getPlayersByPosition(primary);
        if (candidates.isEmpty()) {
            candidates = team.getPlayersByPosition(secondary);
        }
        if (candidates.isEmpty()) {
            candidates = team.players();
        }
        if (candidates.isEmpty()) {
            return new Player("unknown", "Unknown Player", primary, 0, 70);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}

