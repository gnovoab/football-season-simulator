# Football Season Simulator - AI/LLM Context Document

> **IMPORTANT**: Any AI/LLM making changes to this project MUST update this document to reflect those changes.

## Project Overview

**Name**: Football Season Simulator  
**Type**: Real-time football season simulation web application  
**Version**: 0.0.1-SNAPSHOT  
**License**: MIT

### Goal
Simulate continuous football seasons for 5 major European leagues with real-time match updates delivered via WebSocket. Users can watch live match simulations, view standings, and track season progression.

## Supported Leagues

| League | Country | Teams | Matchweeks |
|--------|---------|-------|------------|
| Premier League | England | 20 | 38 |
| La Liga | Spain | 20 | 38 |
| Serie A | Italy | 20 | 38 |
| Bundesliga | Germany | 18 | 34 |
| Ligue 1 | France | 18 | 34 |

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.2 |
| Build Tool | Gradle (Kotlin DSL) | 9.3.0 |
| Database | H2 (in-memory) | - |
| WebSocket | STOMP over SockJS | - |
| Frontend | Thymeleaf + JavaScript | - |
| API Docs | SpringDoc OpenAPI | 2.8.5 |
| Testing | JUnit 5 + Awaitility | - |
| Code Coverage | JaCoCo | 0.8.12 |

## Project Structure

```
src/main/java/com/example/footballseasonsimulator/
├── FootballSeasonSimulatorApplication.java  # Main entry point
├── config/
│   ├── OpenApiConfig.java                   # Swagger/OpenAPI configuration
│   └── WebSocketConfig.java                 # WebSocket STOMP configuration
├── controller/
│   ├── LeagueController.java                # REST API for leagues (/api/v1/leagues)
│   ├── MatchController.java                 # REST API for matches (/api/v1/matches)
│   └── PageController.java                  # Thymeleaf page controller
├── engine/
│   ├── EventGenerator.java                  # Generates match events (goals, cards, etc.)
│   ├── MatchEngine.java                     # Tick-based match simulation
│   ├── RoundRobinScheduler.java             # Creates fixture schedules
│   └── SeasonRunner.java                    # Manages season lifecycle
├── model/
│   ├── Fixture.java                         # Matchweek fixture
│   ├── FootballEventType.java               # Event types enum
│   ├── League.java                          # League record
│   ├── Match.java                           # Match entity
│   ├── MatchEvent.java                      # Match event entity
│   ├── MatchPhase.java                      # Match phase enum
│   ├── Player.java                          # Player record
│   ├── Position.java                        # Player position enum
│   ├── SeasonState.java                     # Season state enum
│   ├── Standing.java                        # League standing
│   ├── Team.java                            # Team record
│   └── TeamStrength.java                    # Team strength ratings
├── service/
│   ├── LeagueDataService.java               # Loads team/player data from JSON
│   ├── SimulationService.java               # Main simulation orchestrator
│   └── StandingsService.java                # Calculates league standings
└── websocket/
    └── MatchEventPublisher.java             # Publishes events via WebSocket
```

## Data Files

Team and player data stored in `src/main/resources/data/`:
- `premier-league.json`
- `la-liga.json`
- `serie-a.json`
- `bundesliga.json`
- `ligue-1.json`

## API Endpoints

### League Endpoints (`/api/v1/leagues`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/leagues` | Get all leagues |
| GET | `/api/v1/leagues/{id}` | Get league by ID |
| GET | `/api/v1/leagues/{id}/standings` | Get league standings |
| GET | `/api/v1/leagues/{id}/fixture` | Get current fixture |
| GET | `/api/v1/leagues/{id}/next-fixture` | Get next fixture |
| GET | `/api/v1/leagues/{id}/status` | Get simulation status |

### Match Endpoints (`/api/v1/matches`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/matches/{id}` | Get match details |
| GET | `/api/v1/matches/{id}/events` | Get match events |
| GET | `/api/v1/matches/{id}/events/significant` | Get significant events only |

## WebSocket Topics

| Topic | Description |
|-------|-------------|
| `/topic/league/{leagueId}/events` | League-specific match events |
| `/topic/league/{leagueId}/matches` | League match state updates |
| `/topic/league/{leagueId}/standings` | League standings updates |
| `/topic/match/{matchId}` | Individual match updates |
| `/topic/events` | Global events feed |

## Key Configuration Variables

### Timing (in MatchEngine.java)
| Variable | Value | Description |
|----------|-------|-------------|
| `TICK_INTERVAL_MS` | 200ms | Simulation tick interval |
| `MINUTES_PER_TICK` | 1.0 | Match minutes per tick |
| `HALF_TIME_MINUTE` | 45 | First half duration |
| `MATCH_DURATION_MINUTES` | 90 | Full match duration |

### Event Probabilities (in EventGenerator.java)
Events are generated based on team strength and random probability calculations.

## Current Status

- ✅ Core simulation engine working
- ✅ All 5 leagues implemented with 2025-26 squads
- ✅ WebSocket real-time updates
- ✅ API versioning (`/api/v1/`)
- ✅ Swagger documentation
- ✅ 202 tests with 97% code coverage
- ✅ Code quality tools (Checkstyle, PMD, SpotBugs, OWASP)
- ✅ Rate limiting (Bucket4j - 100 requests/minute)
- ✅ Caching (Caffeine - fixtures cache with 10s TTL)
- ✅ Metrics (Micrometer with Prometheus registry)
- ✅ Distributed tracing (Micrometer Tracing with Brave/Zipkin)
- ✅ Custom health indicators (simulation status, database)
- ✅ Structured logging (ECS JSON format)
- ✅ CORS configuration (centralized, configurable)
- ✅ Input validation (Bean Validation with RFC 7807 error responses)
- ✅ Statistics API (top scorers, team stats, league summary)
- ✅ Predictions API (win probability, expected goals, event likelihood)

## Running the Application

```bash
# Development
./gradlew bootRun

# Run tests
./gradlew test

# Build JAR
./gradlew build
```

## Access Points

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Main UI |
| http://localhost:8080/swagger-ui.html | API Documentation |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

---

## AI Change Log

| Date | AI/Agent | Changes Made |
|------|----------|--------------|
| 2026-02-15 | Augment Agent | Created initial AI documentation |

---

**REMINDER**: Update this document after making any changes to the project.

