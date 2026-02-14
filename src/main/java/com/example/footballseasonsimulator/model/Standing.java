package com.example.footballseasonsimulator.model;

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
    }

    public void recordResult(int scored, int conceded) {
        this.played++;
        this.goalsFor += scored;
        this.goalsAgainst += conceded;
        
        if (scored > conceded) {
            this.won++;
        } else if (scored < conceded) {
            this.lost++;
        } else {
            this.drawn++;
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

