# Football Season Simulator

A real-time football season simulator that runs continuous simulations of 5 major European leagues with live match updates via WebSocket.

## Table of Contents

- [How to Run the System](#how-to-run-the-system)
- [How to Update Data](#how-to-update-data)
- [API Endpoints](#api-endpoints)
- [System Architecture](#system-architecture)
- [Event Calculation](#event-calculation)
- [Fixture Calculation](#fixture-calculation)
- [WebSocket Events](#websocket-events)

---

## How to Run the System

### Prerequisites

- **Java 21** or higher
- **Gradle 9.x** (wrapper included)

### Running the Application

```bash
# Clone the repository
git clone https://github.com/gnovoab/football-season-simulator.git
cd football-season-simulator

# Run with Gradle wrapper
./gradlew bootRun

# Or build and run the JAR
./gradlew build
java -jar build/libs/football-season-simulator-0.0.1-SNAPSHOT.jar
```

### Access Points

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Main UI |
| http://localhost:8080/swagger-ui.html | Swagger API Documentation |
| http://localhost:8080/v3/api-docs | OpenAPI JSON specification |
| http://localhost:8080/h2-console | H2 Database Console (if enabled) |

---

## How to Update Data

Team and player data is stored in JSON files under `src/main/resources/data/`:

```
src/main/resources/data/
├── premier-league.json
├── la-liga.json
├── serie-a.json
├── bundesliga.json
└── ligue-1.json
```

### JSON Structure

Each league file follows this structure:

```json
{
  "id": "premier-league",
  "name": "Premier League",
  "country": "England",
  "logoUrl": "https://www.thesportsdb.com/images/media/league/badge/i6o0kh1549879062.png",
  "teams": [
    {
      "id": "arsenal",
      "name": "Arsenal FC",
      "shortName": "ARS",
      "badgeUrl": "https://www.thesportsdb.com/images/media/team/badge/...",
      "strength": {
        "attack": 85,
        "midfield": 84,
        "defense": 82,
        "goalkeeper": 83
      },
      "players": [
        {
          "id": "saka",
          "name": "Bukayo Saka",
          "position": "FORWARD",
          "number": 7,
          "rating": 87
        }
      ]
    }
  ]
}
```

### Adding/Updating Teams

1. **Add a new team**: Add a new team object to the `teams` array
2. **Update players**: Modify the `players` array within a team
3. **Adjust strength**: Modify `attack`, `midfield`, `defense`, `goalkeeper` values (0-100)
4. **Promoted/Relegated**: Replace relegated team objects with promoted team objects

### Position Values

- `GOALKEEPER` - Goalkeepers
- `DEFENDER` - Defenders
- `MIDFIELDER` - Midfielders
- `FORWARD` - Forwards

### Strength Values

Values range from 0-100:
- **90-100**: Elite (top 3 teams)
- **80-89**: Strong (top 6-8 teams)
- **70-79**: Mid-table
- **60-69**: Lower table
- **50-59**: Relegation candidates

---

## API Endpoints

> **API Versioning**: All API endpoints use URL-based versioning with the `/api/v1/` prefix.
> This ensures backward compatibility when new API versions are released.

### League Endpoints (`/api/v1/leagues`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/leagues` | Get all leagues |
| GET | `/api/v1/leagues/{leagueId}` | Get specific league details |
| GET | `/api/v1/leagues/{leagueId}/standings` | Get current standings |
| GET | `/api/v1/leagues/{leagueId}/fixtures` | Get current matchweek fixtures |
| GET | `/api/v1/leagues/{leagueId}/results` | Get completed matches |
| GET | `/api/v1/leagues/{leagueId}/status` | Get simulation status |
| GET | `/api/v1/leagues/{leagueId}/next-fixture` | Get next matchweek fixtures |

### Match Endpoints (`/api/v1/matches`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/matches/{matchId}` | Get match details with stats |
| GET | `/api/v1/matches/{matchId}/events` | Get all match events |
| GET | `/api/v1/matches/{matchId}/events/significant` | Get goals, cards, penalties only |

### League IDs

- `premier-league` - English Premier League
- `la-liga` - Spanish La Liga
- `serie-a` - Italian Serie A
- `bundesliga` - German Bundesliga
- `ligue-1` - French Ligue 1

---

## System Architecture

### Overview

The system consists of several key components:

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (UI)                           │
│  Thymeleaf Templates + JavaScript + WebSocket Client            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      REST Controllers                           │
│  LeagueController │ MatchController │ PageController            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Services                                │
│  SimulationService │ LeagueDataService │ StandingsService       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Engine Components                          │
│  MatchEngine │ EventGenerator │ RoundRobinScheduler │ SeasonRunner│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    WebSocket Publisher                          │
│  MatchEventPublisher (STOMP over WebSocket)                     │
└─────────────────────────────────────────────────────────────────┘
```

### Component Descriptions

| Component | Description |
|-----------|-------------|
| **SimulationService** | Orchestrates the entire simulation, manages league lifecycles |
| **LeagueDataService** | Loads team/player data from JSON files |
| **StandingsService** | Calculates and maintains league standings |
| **MatchEngine** | Simulates individual matches with tick-based progression |
| **EventGenerator** | Generates match events (goals, fouls, cards) based on probabilities |
| **RoundRobinScheduler** | Creates fixture schedules using the circle method |
| **MatchEventPublisher** | Broadcasts events via WebSocket to connected clients |

### Timing Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Tick Interval | 200ms | Time between simulation ticks |
| Minutes per Tick | 2 | Match minutes simulated per tick |
| Match Duration | ~75 seconds | Real time for 90-minute match |
| Fixture Gap | 30 seconds | Delay between matchweeks |
| Season Gap | 60 seconds | Delay between seasons |

---

## Event Calculation

The `EventGenerator` class generates match events using probability-based calculations modified by team strengths.

### Base Probabilities (per minute)

| Event | Probability | Description |
|-------|-------------|-------------|
| Shot Attempt | 12% | Base chance of a shot |
| Foul | 1.5% | Base chance of a foul |
| Corner | 1.2% | Base chance of a corner |

### Conversion Rates

| Event | Rate | Description |
|-------|------|-------------|
| Shot on Target | 38% | Percentage of shots on target |
| Goal Conversion | 32% | Percentage of on-target shots that score |
| Save Rate | 65% | Percentage of non-goal shots saved |
| Yellow Card | 12% | Percentage of fouls resulting in yellow |
| Red Card | 0.8% | Percentage of fouls resulting in red |
| Penalty | 2.5% | Percentage of fouls that are penalties |
| Penalty Conversion | 78% | Percentage of penalties scored |

### Attack Modifier Formula

The attack modifier adjusts event probabilities based on team strengths:

```
attackModifier = 0.5 + (attackStrength × 0.25) + (midfieldStrength × 0.15) + (defenseWeakness × 0.1)
```

Where:
- `attackStrength` = attacking team's attack rating / 100
- `midfieldStrength` = attacking team's midfield rating / 100
- `defenseWeakness` = 1 - (defending team's defense rating / 100)

### Goal Chance Formula

```
goalChance = GOAL_CONVERSION_RATE × (attackerAttack / 80) × (80 / defenderGoalkeeper)
```

This produces approximately **2.5-3 goals per match** on average.

---

## Fixture Calculation

The `RoundRobinScheduler` generates fixtures using the **Circle Method** (Berger Tables).

### Algorithm Overview

1. **Double Round-Robin**: Each team plays every other team twice (home and away)
2. **Circle Method**: One team stays fixed, others rotate positions
3. **Home/Away Balance**: Alternates to ensure fair distribution

### For a 20-Team League

- **38 matchweeks** (19 first half + 19 second half)
- **10 matches per matchweek**
- **380 total matches per season**

### First Half Generation

```
Round 1: Fixed team vs Team[0], then pair remaining teams from opposite ends
Round 2: Rotate teams (last → first), repeat pairing
...
Round 19: Final rotation
```

### Second Half Generation

- Takes first half fixtures
- Shuffles the order for variety
- Swaps home/away for each match

### Handling Odd Teams

If a league has an odd number of teams (e.g., Bundesliga with 18):
- A "bye" placeholder is added
- Teams matched against "bye" have a rest week

---

## WebSocket Events

The application uses STOMP over WebSocket for real-time updates.

### Connection

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    // Subscribe to topics
});
```

### Topics

| Topic | Description |
|-------|-------------|
| `/topic/events/{leagueId}` | Match events (goals, fouls, cards) |
| `/topic/matches/{leagueId}` | Match state updates (score, time) |
| `/topic/standings/{leagueId}` | Standings updates |
| `/topic/fixtures/{leagueId}` | Fixture start notifications |
| `/topic/status/{leagueId}` | Simulation status changes |
| `/topic/countdown/{leagueId}` | Pre-match countdown |

### Event Types

```java
public enum FootballEventType {
    KICK_OFF, HALF_TIME, FULL_TIME,
    GOAL, OWN_GOAL, PENALTY_SCORED,
    SHOT_ON_TARGET, SHOT_OFF_TARGET, SAVE,
    FOUL, YELLOW_CARD, RED_CARD,
    CORNER_KICK, FREE_KICK, OFFSIDE,
    PENALTY_AWARDED, PENALTY_MISSED, PENALTY_SAVED,
    SUBSTITUTION, INJURY
}
```

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 4.0.2 | Application framework |
| Spring WebSocket | - | Real-time communication |
| Thymeleaf | - | Server-side templating |
| H2 Database | - | In-memory database |
| Jackson | - | JSON processing |
| SpringDoc OpenAPI | 2.8.5 | API documentation |

---

## License

MIT License

