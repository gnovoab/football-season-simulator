package com.example.footballseasonsimulator.engine;

import com.example.footballseasonsimulator.model.Fixture;
import com.example.footballseasonsimulator.model.League;
import com.example.footballseasonsimulator.model.Match;
import com.example.footballseasonsimulator.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates fixtures for a double round-robin tournament.
 * Uses the circle method (Berger tables) for scheduling.
 * 
 * For a 20-team league:
 * - 38 matchweeks (19 home, 19 away)
 * - 10 matches per matchweek
 * - Each team plays every other team twice (home and away)
 */
public class RoundRobinScheduler {

    /**
     * Generate all fixtures for a season.
     * 
     * @param league The league to generate fixtures for
     * @param season The season number
     * @return List of fixtures (matchweeks) for the entire season
     */
    public List<Fixture> generateSeasonFixtures(League league, int season) {
        List<Team> teams = new ArrayList<>(league.teams());
        int numTeams = teams.size();
        
        if (numTeams < 2) {
            throw new IllegalArgumentException("Need at least 2 teams for a league");
        }
        
        // If odd number of teams, add a "bye" placeholder
        boolean hasOddTeams = numTeams % 2 != 0;
        if (hasOddTeams) {
            teams.add(null); // null represents a bye
            numTeams++;
        }
        
        List<Fixture> firstHalf = generateFirstHalfFixtures(league.id(), season, teams);
        List<Fixture> secondHalf = generateSecondHalfFixtures(league.id(), season, teams, firstHalf);
        
        List<Fixture> allFixtures = new ArrayList<>();
        allFixtures.addAll(firstHalf);
        allFixtures.addAll(secondHalf);
        
        return allFixtures;
    }

    /**
     * Generate first half of season (matchweeks 1-19 for 20 teams).
     * Uses the circle method where one team stays fixed and others rotate.
     */
    private List<Fixture> generateFirstHalfFixtures(String leagueId, int season, List<Team> teams) {
        List<Fixture> fixtures = new ArrayList<>();
        int numTeams = teams.size();
        int numRounds = numTeams - 1;
        
        // Create a working copy for rotation
        List<Team> rotatingTeams = new ArrayList<>(teams.subList(1, numTeams));
        Team fixedTeam = teams.get(0);
        
        for (int round = 0; round < numRounds; round++) {
            List<Match> matches = new ArrayList<>();
            int matchweek = round + 1;
            
            // First match: fixed team vs first rotating team
            Team home = (round % 2 == 0) ? fixedTeam : rotatingTeams.get(0);
            Team away = (round % 2 == 0) ? rotatingTeams.get(0) : fixedTeam;
            
            if (home != null && away != null) {
                matches.add(new Match(leagueId, season, matchweek, home, away));
            }
            
            // Remaining matches: pair teams from opposite ends of rotating list
            for (int i = 1; i < numTeams / 2; i++) {
                int homeIdx = i;
                int awayIdx = numTeams - 1 - i;
                
                home = rotatingTeams.get(homeIdx);
                away = rotatingTeams.get(awayIdx);
                
                // Alternate home/away based on round
                if (i % 2 == round % 2) {
                    Team temp = home;
                    home = away;
                    away = temp;
                }
                
                if (home != null && away != null) {
                    matches.add(new Match(leagueId, season, matchweek, home, away));
                }
            }
            
            fixtures.add(new Fixture(leagueId, season, matchweek, matches));
            
            // Rotate: move last element to front
            rotatingTeams.add(0, rotatingTeams.remove(rotatingTeams.size() - 1));
        }
        
        return fixtures;
    }

    /**
     * Generate second half of season by reversing home/away from first half.
     */
    private List<Fixture> generateSecondHalfFixtures(String leagueId, int season, 
                                                      List<Team> teams, List<Fixture> firstHalf) {
        List<Fixture> secondHalf = new ArrayList<>();
        int numFirstHalfRounds = firstHalf.size();
        
        // Shuffle the order of second half matchweeks for variety
        List<Integer> secondHalfOrder = new ArrayList<>();
        for (int i = 0; i < numFirstHalfRounds; i++) {
            secondHalfOrder.add(i);
        }
        Collections.shuffle(secondHalfOrder);
        
        for (int i = 0; i < numFirstHalfRounds; i++) {
            int sourceIdx = secondHalfOrder.get(i);
            Fixture sourceFixture = firstHalf.get(sourceIdx);
            int matchweek = numFirstHalfRounds + i + 1;
            
            List<Match> reversedMatches = new ArrayList<>();
            for (Match original : sourceFixture.matches()) {
                // Swap home and away
                reversedMatches.add(new Match(
                    leagueId, season, matchweek,
                    original.getAwayTeam(),  // Now home
                    original.getHomeTeam()   // Now away
                ));
            }
            
            secondHalf.add(new Fixture(leagueId, season, matchweek, reversedMatches));
        }
        
        return secondHalf;
    }
}

