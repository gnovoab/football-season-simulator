package com.example.footballseasonsimulator.health;

import com.example.footballseasonsimulator.model.League;
import com.example.footballseasonsimulator.service.SimulationService;
import com.example.footballseasonsimulator.service.SimulationService.SimulationStatus;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom health indicator for the football simulation.
 * 
 * <p>Reports the health status of all league simulations including:
 * <ul>
 *   <li>Number of active leagues</li>
 *   <li>Current state of each league simulation</li>
 *   <li>Current season and matchweek for each league</li>
 * </ul>
 * 
 * <p>Health is considered UP if at least one league is actively simulating.
 */
@Component
public class SimulationHealthIndicator implements HealthIndicator {

    private final SimulationService simulationService;

    public SimulationHealthIndicator(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Override
    public Health health() {
        List<League> leagues = simulationService.getAllLeagues();
        
        if (leagues.isEmpty()) {
            return Health.down()
                    .withDetail("error", "No leagues configured")
                    .build();
        }

        Map<String, Object> details = new HashMap<>();
        int activeCount = 0;
        
        for (League league : leagues) {
            SimulationStatus status = simulationService.getStatus(league.id());
            if (status != null) {
                Map<String, Object> leagueDetails = new HashMap<>();
                leagueDetails.put("state", status.state().name());
                leagueDetails.put("season", status.season());
                leagueDetails.put("matchweek", status.currentMatchweek() + "/" + status.totalMatchweeks());
                details.put(league.name(), leagueDetails);
                activeCount++;
            }
        }

        details.put("activeLeagues", activeCount);
        details.put("totalLeagues", leagues.size());

        if (activeCount == leagues.size()) {
            return Health.up()
                    .withDetails(details)
                    .build();
        } else if (activeCount > 0) {
            return Health.status("DEGRADED")
                    .withDetails(details)
                    .build();
        } else {
            return Health.down()
                    .withDetail("error", "No active simulations")
                    .withDetails(details)
                    .build();
        }
    }
}

