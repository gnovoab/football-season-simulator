# Development Guide for AI/LLM Agents

> **IMPORTANT**: Any AI/LLM making changes to this project MUST update the AI documentation to reflect those changes.

## Before Making Changes

1. **Read the documentation** in this `AI/` folder
2. **Understand the architecture** - see `ARCHITECTURE.md`
3. **Run existing tests** to ensure baseline: `./gradlew test`
4. **Check code coverage**: `./gradlew jacocoTestReport`

## Making Changes

### Adding New Features

1. Follow existing patterns in the codebase
2. Add appropriate tests (aim for >90% coverage)
3. Update Swagger annotations for new API endpoints
4. Update this documentation

### Modifying Existing Code

1. Understand the impact on dependent components
2. Update affected tests
3. Run full test suite before committing

## Code Style & Quality

### Tools Configured
- **Checkstyle**: Code style enforcement
- **PMD**: Static code analysis
- **SpotBugs**: Bug detection
- **JaCoCo**: Code coverage (90% minimum threshold)
- **OWASP Dependency Check**: Security vulnerability scanning

### Running Quality Checks
```bash
# Run all checks
./gradlew check

# Individual checks
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew spotbugsMain
./gradlew jacocoTestCoverageVerification
./gradlew dependencyCheckAnalyze
```

## Testing

### Test Structure
```
src/test/java/com/example/footballseasonsimulator/
├── controller/           # Controller integration tests
├── engine/              # Engine unit tests
├── model/               # Model unit tests
├── service/             # Service unit tests
└── websocket/           # WebSocket unit tests
```

### Running Tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "ClassName"

# With coverage report
./gradlew test jacocoTestReport
```

### Test Approach
- Use `@SpringBootTest` for integration tests
- Use `@AutoConfigureMockMvc` for controller tests
- Use `Awaitility` for async testing

## API Guidelines

### Versioning
- All REST endpoints use `/api/v1/` prefix
- WebSocket endpoints are not versioned

### Adding New Endpoints
1. Add method to appropriate controller
2. Add Swagger annotations (`@Operation`, `@ApiResponse`, etc.)
3. Add integration test
4. Update `AI/PROJECT_CONTEXT.md` API table

## Common Tasks

### Adding a New League
1. Create JSON file in `src/main/resources/data/`
2. Update `LeagueDataService` to load the new file
3. Add tests

### Modifying Event Probabilities
- Edit `EventGenerator.java`
- Probabilities are based on team strength ratings

### Changing Simulation Speed
- Edit constants in `MatchEngine.java`:
  - `TICK_INTERVAL_MS` - How often simulation ticks
  - `MINUTES_PER_TICK` - Match minutes per tick

## Git Workflow

### Commit Messages
Use descriptive commit messages:
```
Add [feature] - Brief description

- Detail 1
- Detail 2
```

### Before Committing
1. Run tests: `./gradlew test`
2. Check coverage: `./gradlew jacocoTestCoverageVerification`
3. Update AI documentation if needed

## Troubleshooting

### App Won't Start
- Check port 8080 is available
- Check Java 21 is installed
- Run `./gradlew clean bootRun`

### Tests Failing
- Run `./gradlew clean test` for fresh build
- Check for async timing issues (use Awaitility)

### WebSocket Not Working
- Check browser console for connection errors
- Verify `/ws` endpoint is accessible

## Dependencies

### Adding Dependencies
Use Gradle commands, not manual editing:
```bash
# Example (conceptual - use appropriate Gradle syntax)
# Add to build.gradle.kts dependencies block
```

### Current Key Dependencies
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-websocket` - WebSocket support
- `spring-boot-starter-thymeleaf` - Template engine
- `springdoc-openapi-starter-webmvc-ui` - Swagger UI

---

**Last Updated**: 2026-02-15 by Augment Agent

