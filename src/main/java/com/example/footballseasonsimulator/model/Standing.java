package com.example.footballseasonsimulator.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a team's standing in the league table.
 */
public class Standing implements Comparable<Standing> {
    private final String teamId;
    private final String teamName;
    private final String teamBadgeUrl;
    private int position;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private final List<Character> form; // Last 5 results: 'W', 'D', 'L'

    public Standing(Team team) {
        this.teamId = team.id();
        this.teamName = team.name();
        this.teamBadgeUrl = team.badgeUrl();
        this.position = 0;
        this.played = 0;
        this.won = 0;
        this.drawn = 0;
        this.lost = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
        this.form = new ArrayList<>();
    }

    /**
     * Create a standing with all values set directly (for live standings calculation).
     */
    public static Standing createWithValues(String teamId, String teamName, String teamBadgeUrl,
                                            int played, int won, int drawn, int lost,
                                            int goalsFor, int goalsAgainst, List<Character> form) {
        // Create a minimal team just for the standing
        Team minimalTeam = new Team(teamId, teamName, teamName, teamBadgeUrl,
                                    new TeamStrength(50, 50, 50, 50), List.of());
        Standing s = new Standing(minimalTeam);
        s.played = played;
        s.won = won;
        s.drawn = drawn;
        s.lost = lost;
        s.goalsFor = goalsFor;
        s.goalsAgainst = goalsAgainst;
        s.form.addAll(form);
        return s;
    }

    public void recordResult(int scored, int conceded) {
        this.played++;
        this.goalsFor += scored;
        this.goalsAgainst += conceded;

        char result;
        if (scored > conceded) {
            this.won++;
            result = 'W';
        } else if (scored < conceded) {
            this.lost++;
            result = 'L';
        } else {
            this.drawn++;
            result = 'D';
        }

        // Add to form and keep only last 5
        form.add(result);
        if (form.size() > 5) {
            form.remove(0);
        }
    }

    public int getPoints() {
        return (won * 3) + drawn;
    }

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }

    // Getters
    public String getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public String getTeamBadgeUrl() { return teamBadgeUrl; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public List<Character> getForm() { return new ArrayList<>(form); }
    public String getFormString() {
        StringBuilder sb = new StringBuilder();
        for (Character c : form) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(Standing other) {
        // Sort by: points (desc), goal difference (desc), goals for (desc), name (asc)
        int pointsDiff = Integer.compare(other.getPoints(), this.getPoints());
        if (pointsDiff != 0) return pointsDiff;
        
        int gdDiff = Integer.compare(other.getGoalDifference(), this.getGoalDifference());
        if (gdDiff != 0) return gdDiff;
        
        int gfDiff = Integer.compare(other.goalsFor, this.goalsFor);
        if (gfDiff != 0) return gfDiff;
        
        return this.teamName.compareTo(other.teamName);
    }
}

