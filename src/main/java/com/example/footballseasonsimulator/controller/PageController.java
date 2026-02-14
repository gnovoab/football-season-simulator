package com.example.footballseasonsimulator.controller;

import com.example.footballseasonsimulator.model.League;
import com.example.footballseasonsimulator.service.SimulationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Controller for serving Thymeleaf pages.
 */
@Controller
public class PageController {
    
    private final SimulationService simulationService;
    
    public PageController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }
    
    /**
     * Main page - shows all leagues.
     */
    @GetMapping("/")
    public String index(Model model) {
        List<League> leagues = simulationService.getAllLeagues();
        model.addAttribute("leagues", leagues);
        
        // Default to first league
        if (!leagues.isEmpty()) {
            model.addAttribute("selectedLeague", leagues.get(0));
        }
        
        return "index";
    }
    
    /**
     * League page - shows specific league.
     */
    @GetMapping("/league/{leagueId}")
    public String league(@PathVariable String leagueId, Model model) {
        List<League> leagues = simulationService.getAllLeagues();
        model.addAttribute("leagues", leagues);
        
        League selectedLeague = simulationService.getLeague(leagueId);
        if (selectedLeague == null && !leagues.isEmpty()) {
            selectedLeague = leagues.get(0);
        }
        model.addAttribute("selectedLeague", selectedLeague);
        
        return "index";
    }
}

