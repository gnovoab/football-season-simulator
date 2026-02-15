package com.example.footballseasonsimulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context loading test.
 * Rate limiting is disabled via the 'test' profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class FootballSeasonSimulatorApplicationTests {

    @Test
    void contextLoads() {
    }

}
