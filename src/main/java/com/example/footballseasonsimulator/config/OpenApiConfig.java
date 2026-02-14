package com.example.footballseasonsimulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Configures API versioning, metadata, and server information.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI footballSeasonSimulatorOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("Football Season Simulator API")
                .version("1.0.0")
                .description("""
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
                        """)
                .contact(new Contact()
                        .name("Football Season Simulator")
                        .url("https://github.com/example/football-season-simulator"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
}

