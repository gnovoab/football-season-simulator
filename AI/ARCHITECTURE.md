# Architecture Documentation

> **IMPORTANT**: Any AI/LLM making changes to this project MUST update this document to reflect those changes.

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (UI)                           │
│  Thymeleaf Templates + JavaScript + WebSocket Client            │
│  Location: src/main/resources/templates/ & static/              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      REST Controllers                           │
│  LeagueController │ MatchController │ PageController            │
│  Base Path: /api/v1/                                            │
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
│  Endpoint: /ws                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

### Controllers

| Controller | Responsibility |
|------------|----------------|
| `LeagueController` | REST API for league data, standings, fixtures, status |
| `MatchController` | REST API for individual match details and events |
| `PageController` | Serves Thymeleaf HTML pages |

### Services

| Service | Responsibility |
|---------|----------------|
| `SimulationService` | Main orchestrator - manages all league simulations, lifecycle |
| `LeagueDataService` | Loads team/player data from JSON files at startup |
| `StandingsService` | Calculates league standings from match results |

### Engine Components

| Component | Responsibility |
|-----------|----------------|
| `MatchEngine` | Tick-based match simulation, processes minutes, generates events |
| `EventGenerator` | Generates match events based on probabilities and team strength |
| `RoundRobinScheduler` | Creates fixture schedules using circle method algorithm |
| `SeasonRunner` | Manages season lifecycle, coordinates matchweeks |

### WebSocket

| Component | Responsibility |
|-----------|----------------|
| `MatchEventPublisher` | Broadcasts events and state updates to WebSocket subscribers |
| `WebSocketConfig` | Configures STOMP broker and endpoints |

## Data Flow

### Match Simulation Flow
```
1. SimulationService starts season for each league
2. SeasonRunner manages matchweek progression
3. For each match in fixture:
   a. MatchEngine.startMatch() initializes match
   b. MatchEngine.tick() called every 200ms
   c. EventGenerator generates events for each minute
   d. MatchEventPublisher broadcasts events via WebSocket
   e. StandingsService updates standings after match
4. After all matches complete, next matchweek begins
5. After all matchweeks, new season starts
```

### API Request Flow
```
Client Request → Controller → SimulationService → Return Data
```

### WebSocket Flow
```
MatchEngine → MatchEventPublisher → STOMP Broker → WebSocket Clients
```

## Threading Model

- **Main Thread**: Spring Boot application startup
- **Simulation Threads**: One `ScheduledExecutorService` per league
- **WebSocket Threads**: Managed by Spring's `SimpMessagingTemplate`

## State Management

All state is held in-memory within `SimulationService`:
- `Map<String, LeagueSimulation>` - Active league simulations
- Each `LeagueSimulation` contains current season, fixtures, standings

## Database

Currently uses H2 in-memory database (configured but minimally used).
Primary data storage is in-memory for real-time performance.

---

**Last Updated**: 2026-02-15 by Augment Agent

