# Football Season Simulator

A real-time football season simulator that runs continuous simulations of 5 major European leagues with live match updates via WebSocket.

## Table of Contents

- [AI/LLM Agents](#aillm-agents)
- [How to Run the System](#how-to-run-the-system)
- [How to Update Data](#how-to-update-data)
- [API Endpoints](#api-endpoints)
- [Monitoring and Observability](#monitoring-and-observability)
- [System Architecture](#system-architecture)
- [Event Calculation](#event-calculation)
- [Fixture Calculation](#fixture-calculation)
- [WebSocket Events](#websocket-events)

---

## AI/LLM Agents

> **For AI assistants, LLMs, or coding agents working on this project:**

Before making any changes to this codebase, please read the documentation in the `AI/` folder:

```
AI/
├── PROJECT_CONTEXT.md    # Project overview, goals, features, requirements, and current status
├── ARCHITECTURE.md       # System architecture, component responsibilities, data flow, threading model
└── DEVELOPMENT_GUIDE.md  # Development guidelines, testing approach, common tasks, git workflow
```

### Context for AI Agents

When starting work on this project, use the following prompt or context:

```
Read the AI documentation folder to understand this project:
- AI/PROJECT_CONTEXT.md - for project goals, features, and current status
- AI/ARCHITECTURE.md - for system architecture and component details
- AI/DEVELOPMENT_GUIDE.md - for development guidelines and testing approach

Key points:
- This is a Spring Boot 4.0.2 application with Java 21
- Real-time football simulation with WebSocket updates
- API endpoints are versioned under /api/v1/
- Tests use @SpringBootTest for integration testing
- Code coverage minimum is 90% (JaCoCo)
- Any changes must be documented in the AI/ folder
```

### Important Requirements

1. **Read before coding**: Always read the `AI/` folder documentation before making changes
2. **Update documentation**: Any architectural or significant changes must be reflected in the AI documentation
3. **Run tests**: Execute `./gradlew test` before committing changes
4. **Maintain coverage**: Ensure code coverage stays above 90%

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

### Statistics Endpoints (`/api/v1/statistics`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/statistics/{leagueId}/top-scorers` | Get top goal scorers in a league |
| GET | `/api/v1/statistics/{leagueId}/teams/{teamId}` | Get detailed team statistics |
| GET | `/api/v1/statistics/{leagueId}/summary` | Get league-wide statistics summary |

#### Top Scorers Response Example

```json
[
  {
    "playerId": "player-123",
    "playerName": "Erling Haaland",
    "teamId": "manchester-city",
    "teamName": "Manchester City",
    "goals": 15,
    "yellowCards": 1,
    "redCards": 0,
    "appearances": 12
  }
]
```

#### Team Statistics Response Example

```json
{
  "teamId": "arsenal",
  "teamName": "Arsenal FC",
  "badgeUrl": "https://...",
  "played": 15,
  "won": 10,
  "drawn": 3,
  "lost": 2,
  "goalsFor": 32,
  "goalsAgainst": 12,
  "goalDifference": 20,
  "points": 33,
  "form": "WWDWL",
  "totalShots": 180,
  "shotsOnTarget": 72,
  "corners": 95,
  "fouls": 145,
  "yellowCards": 18,
  "redCards": 1
}
```

### Predictions Endpoints (`/api/v1/predictions`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/predictions/matches/{matchId}` | Get predictions for a specific match |
| GET | `/api/v1/predictions/head-to-head?leagueId=...&homeTeamId=...&awayTeamId=...` | Get predictions for hypothetical match |

#### Match Prediction Response Example

```json
{
  "matchId": "12345678-1234-1234-1234-123456789012",
  "homeTeamId": "arsenal",
  "homeTeamName": "Arsenal FC",
  "awayTeamId": "chelsea",
  "awayTeamName": "Chelsea FC",
  "winProbability": {
    "homeWin": 45,
    "draw": 28,
    "awayWin": 27
  },
  "expectedGoals": {
    "homeXG": 1.65,
    "awayXG": 1.23,
    "predictedHomeGoals": 2,
    "predictedAwayGoals": 1
  },
  "corners": {
    "homeCorners": 6,
    "awayCorners": 5,
    "totalCorners": 11
  },
  "eventLikelihood": {
    "btts": 55,
    "over25Goals": 60,
    "over35Goals": 35,
    "homeCleanSheet": 28,
    "awayCleanSheet": 22,
    "redCard": 8,
    "penalty": 15
  }
}
```

### League IDs

- `premier-league` - English Premier League
- `la-liga` - Spanish La Liga
- `serie-a` - Italian Serie A
- `bundesliga` - German Bundesliga
- `ligue-1` - French Ligue 1

---

## Monitoring and Observability

The application includes comprehensive monitoring capabilities using Spring Boot Actuator, Micrometer metrics, and distributed tracing.

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status with details |
| `/actuator/info` | Application information |
| `/actuator/metrics` | All available metrics |
| `/actuator/prometheus` | Metrics in Prometheus format |

### Custom Health Indicators

The application includes custom health indicators for monitoring simulation status:

| Indicator | Description |
|-----------|-------------|
| `simulation` | Reports status of all league simulations (state, season, matchweek) |
| `database` | Verifies H2 database connectivity |

Example health response:
```json
{
  "status": "UP",
  "components": {
    "simulation": {
      "status": "UP",
      "details": {
        "Premier League": {"state": "RUNNING_FIXTURE", "season": 1, "matchweek": "5/38"},
        "La Liga": {"state": "WAITING_NEXT_FIXTURE", "season": 1, "matchweek": "4/38"},
        "activeLeagues": 5,
        "totalLeagues": 5
      }
    },
    "database": {
      "status": "UP",
      "details": {"database": "H2", "status": "Connected"}
    }
  }
}
```

### Custom Metrics

The following custom metrics are exposed for monitoring simulation performance:

| Metric | Type | Description |
|--------|------|-------------|
| `simulation.matches.completed` | Counter | Total number of completed matches |
| `simulation.goals.scored` | Counter | Total number of goals scored |
| `simulation.events.generated` | Counter | Events generated (tagged by type) |
| `simulation.active.leagues` | Gauge | Number of active league simulations |
| `simulation.websocket.connections` | Gauge | Active WebSocket connections |
| `simulation.match.duration` | Timer | Time to simulate a match |
| `simulation.fixture.duration` | Timer | Time to simulate a fixture/matchweek |

### Prometheus Integration

To scrape metrics with Prometheus, add this to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'football-simulator'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Distributed Tracing

The application supports distributed tracing using Micrometer Tracing with Zipkin export.

#### Trace Propagation

All HTTP requests and WebSocket messages include trace context:
- `traceId` - Unique identifier for the entire request chain
- `spanId` - Unique identifier for each operation

#### Zipkin Integration

Traces are exported to Zipkin. To run Zipkin locally:

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

Access the Zipkin UI at: http://localhost:9411

#### Configuration

Tracing is configured in `application.yaml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of requests (reduce in production)
```

### Structured Logging

The application uses Spring Boot 4's built-in structured logging with **Elastic Common Schema (ECS)** JSON format. This provides machine-readable logs that are easily ingested by log aggregation systems like Elasticsearch, Splunk, or Datadog.

#### Log Format

Each log entry is a JSON object with the following structure:

```json
{
  "@timestamp": "2026-02-15T19:50:36.030675Z",
  "log": {
    "level": "INFO",
    "logger": "com.example.footballseasonsimulator.service.SimulationService"
  },
  "process": {
    "pid": 66897,
    "thread": {"name": "main"}
  },
  "service": {
    "name": "football-season-simulator",
    "version": "1.0.0",
    "environment": "development"
  },
  "message": "Starting simulation for Premier League",
  "ecs": {"version": "8.11"}
}
```

#### Supported Formats

Spring Boot 4 supports multiple structured logging formats:

| Format | Property Value | Description |
|--------|----------------|-------------|
| ECS | `ecs` | Elastic Common Schema (default) |
| Logstash | `logstash` | Logstash JSON format |
| GELF | `gelf` | Graylog Extended Log Format |

#### Configuration

Structured logging is configured in `application.yaml`:

```yaml
logging:
  structured:
    format:
      console: ecs  # or logstash, gelf
    ecs:
      service:
        name: ${spring.application.name}
        version: 1.0.0
        environment: ${ENVIRONMENT:development}
```

#### Correlation IDs

When distributed tracing is enabled, trace and span IDs are automatically included in the structured logs, enabling correlation across services.

### CORS Configuration

The application includes centralized CORS (Cross-Origin Resource Sharing) configuration for production use.

#### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `cors.allowed-origins` | `*` | Comma-separated list of allowed origins |
| `cors.allowed-methods` | `GET,POST,PUT,DELETE,OPTIONS` | Allowed HTTP methods |
| `cors.allowed-headers` | `*` | Allowed request headers |
| `cors.allow-credentials` | `false` | Whether to allow credentials |
| `cors.max-age` | `3600` | Preflight cache duration (seconds) |

#### Production Configuration

For production, specify explicit allowed origins:

```yaml
cors:
  allowed-origins: https://example.com,https://app.example.com
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: Authorization,Content-Type,X-Requested-With
  allow-credentials: true
  max-age: 3600
```

#### Endpoints Covered

CORS is configured for:
- `/api/**` - All REST API endpoints
- `/actuator/**` - Actuator endpoints (GET only)

### Input Validation

The application uses Bean Validation (Jakarta Validation) to validate API inputs and provide consistent error responses.

#### Validation Annotations

| Annotation | Usage | Description |
|------------|-------|-------------|
| `@NotBlank` | Path variables | Ensures the value is not null or empty |
| `@Pattern` | Path variables | Validates format using regex patterns |
| `@Validated` | Controllers | Enables method-level validation |

#### Validated Parameters

| Parameter | Pattern | Example |
|-----------|---------|---------|
| `leagueId` | `^[a-z0-9-]+$` | `premier-league`, `la-liga` |
| `matchId` | `^[a-f0-9-]{36}$` | `12345678-1234-1234-1234-123456789012` |

#### Error Response Format (RFC 7807)

Validation errors return RFC 7807 Problem Details format:

```json
{
  "type": "https://api.football-simulator.com/errors/constraint-violation",
  "title": "Invalid Request Parameter",
  "status": 400,
  "detail": "Match ID must be a valid UUID",
  "timestamp": "2026-02-15T12:00:00Z"
}
```

#### Error Types

| Error Type | HTTP Status | Description |
|------------|-------------|-------------|
| `validation` | 400 | Request body validation failed |
| `constraint-violation` | 400 | Path/query parameter validation failed |
| `invalid-argument` | 400 | Invalid argument provided |
| `internal` | 500 | Unexpected server error |

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

