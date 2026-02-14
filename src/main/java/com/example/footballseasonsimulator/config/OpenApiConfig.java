package com.example.footballseasonsimulator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Configures API versioning, metadata, and server information.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Football Season Simulator API",
                version = "1.0.0",
                description = """
                        Real-time football season simulator API for 5 major European leagues.

                        ## Features
                        - Live match simulation with real-time updates via WebSocket
                        - League standings and statistics
                        - Match details and events
                        - Season progression tracking

                        ## Supported Leagues
                        - Premier League (England)
                        - La Liga (Spain)
                        - Serie A (Italy)
                        - Bundesliga (Germany)
                        - Ligue 1 (France)

                        ## API Versioning
                        This API uses URL-based versioning. The current version is **v1**.
                        All endpoints are prefixed with `/api/v1/`.

                        ## WebSocket
                        Real-time updates are available via WebSocket at `/ws`.
                        Subscribe to `/topic/matches/{leagueId}` for live match updates.
                        """,
                contact = @Contact(
                        name = "Football Season Simulator",
                        url = "https://github.com/example/football-season-simulator"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server")
        }
)
public class OpenApiConfig {
    // Configuration is done via annotations above
}

