package com.example.footballseasonsimulator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PageController.
 * Tests Thymeleaf page rendering with real services.
 * Rate limiting is disabled via the 'test' profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / should return index page with leagues")
    void indexShouldReturnIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("leagues"))
                .andExpect(model().attributeExists("selectedLeague"));
    }

    @Test
    @DisplayName("GET /league/premier-league should return index with selected league")
    void leagueShouldReturnIndexWithPremierLeague() throws Exception {
        mockMvc.perform(get("/league/premier-league"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("leagues"))
                .andExpect(model().attributeExists("selectedLeague"));
    }

    @Test
    @DisplayName("GET /league/la-liga should return index")
    void leagueShouldReturnIndexWithLaLiga() throws Exception {
        mockMvc.perform(get("/league/la-liga"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /league/serie-a should return index")
    void leagueShouldReturnIndexWithSerieA() throws Exception {
        mockMvc.perform(get("/league/serie-a"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /league/bundesliga should return index")
    void leagueShouldReturnIndexWithBundesliga() throws Exception {
        mockMvc.perform(get("/league/bundesliga"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /league/ligue-1 should return index")
    void leagueShouldReturnIndexWithLigue1() throws Exception {
        mockMvc.perform(get("/league/ligue-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /league/non-existent should fallback")
    void leagueShouldFallbackForNonExistent() throws Exception {
        mockMvc.perform(get("/league/non-existent"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}

